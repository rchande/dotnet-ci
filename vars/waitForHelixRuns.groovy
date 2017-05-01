import groovy.json.JsonSlurper
import hudson.Util
// Given a set of launched Helix runs,
// wait for them to finish.  If running a PR, also reports the
// the state of the runs on the PR.
// Expects a json blob with the form:
// [
//  {
//      CorrelationId
//      QueueId
//      QueueTimeUtc
//  },
//  {
//      CorrelationId
//      QueueId
//      QueueTimeUtc
//  }
// ]

def call (def helixRunsBlob, String prStatusPrefix) {
    // Parallel stages that wait for the runs.
    def helixRunTasks = [:]

    for (int i = 0; i < helixRunsBlob.size(); i++) {
        def currentRun = helixRunsBlob[i];
        def queueId = currentRun['QueueId']
        def correlationId = currentRun['CorrelationId']
        def context = "${prStatusPrefix} - ${queueId}"
        helixRunTasks[queueId] = {
            // State to minimize status updates.
            // 0 = Not yet updated/started
            // 1 = Pending updated
            // 2 = Started updated
            int state = 0;

            // Wait until the Helix runs complete.
            waitUntil (minRecurrencePeriod: 60, maxRecurrencePeriod: 60, unit: 'SECONDS') {
                // Check the state against the Helix API
                def statusUrl = "https://helix.int-dot.net/api/2017-04-14/jobs/${correlationId}/details"
                def statusResponse = httpRequest statusUrl
                def statusContent = (new JsonSlurper()).parseText(statusResponse.content)

                // If the job info hasn't been propagated to the helix api, then we need to wait around.
                boolean isNotStarted = statusContent.JobList == null
                boolean isPending = !isNotStarted && statusContent.WorkItems.Running == 0 && statusContent.WorkItems.Finished == 0
                boolean isFinished = !isNotStarted && statusContent.WorkItems.Unscheduled == 0 && statusContent.WorkItems.Waiting == 0 && statusContent.WorkItems.Running == 0
                boolean someFinished = statusContent.WorkItems.Finished > 0
                boolean isRunning = !isNotStarted && !isPending && !isFinished
                // Construct the link to the results page.
                def mcResultsUrl = "https://mc.int-dot.net/#/user/${Util.rawEncode(statusContent.Creator)}/${Util.rawEncode(statusContent.Source)}/${Util.rawEncode(statusContent.Type)}/${Util.rawEncode(statusContent.Build)}"
                statusContent = null

                def resultValue
                def subMessage
                // If it's running, grab the current state of results too
                if (isRunning || isFinished) {
                    // Check the results
                    // We check the results by going to the API aggregating by correlation id
                    def resultsUrl = "https://helix.int-dot.net/api/2017-04-14/aggregate/jobs?groupBy=job.name&maxResultSets=1&filter.name=${correlationId}"
                    def resultsResponse = httpRequest resultsUrl
                    def resultsContent = (new JsonSlurper()).parseText(resultsResponse.content)

                    // Example content
                    // If the data isn't complete, then Analysis will be empty.
                    /*[
                        {
                            "Key": {
                                "job.name": "0715528c-a31f-46ac-963e-8679c5880dc8"
                            },
                            "Data": {
                                "Analysis": [
                                    {
                                        "Name": "xunit",
                                        "Status": {
                                            "pass": 374595,
                                            "fail": 4,
                                            "skip": 234
                                        }
                                    }
                                ],
                                "WorkItemStatus": {
                                    "run": 1,
                                    "pass": 204
                                }
                            }
                        }
                    ]*/
                    assert resultsContent.size() == 1 : "No results found for helix results API"
                    assert resultsContent[0].Data != null : "No data found in first result for helix results API"

                    if (resultsContent[0].Data.Analysis.size() == 0) {
                        subMessage = "No results yet"
                    }
                    else {
                        assert resultsContent[0].Data.Analysis.size() == 1 : "More than one set of analysis results"
                        assert resultsContent[0].Data.Analysis[0].Name == "xunit" : "Data in results api not xunit format"
                        def passedTests = resultsContent[0].Data.Analysis[0].Status.pass
                        def failedTests = resultsContent[0].Data.Analysis[0].Status.fail
                        def skippedTests = resultsContent[0].Data.Analysis[0].Status.skip
                        def totalTests = passedTests + failedTests + skippedTests

                        def preStatus = isRunning ? "Running - " : ""
                        // Compute the current resultValue.  We'll update the sub result every time, but the final result only when isFinished is true
                        if (failedTests != 0) {
                            resultValue = "FAILURE"
                            subMessage = "${preStatus}Failed ${failedTests}/${totalTests} (${skippedTests} skipped)"
                        }
                        else {
                            resultValue = "SUCCESS"
                            subMessage = "${preStatus}Passed ${passedTests} (${skippedTests} skipped)"
                        }
                    }
                    resultsContent = null
                }

                // We can also grab the info necessary to construct the link for Mission Control from this API.

                if (isPending && state == 0) {
                    state = 1
                    setPRStatus(context, "PENDING", "", "Waiting")
                }
                else if (isRunning && state < 2) {
                    state = 2
                    setPRStatus(context, "PENDING", mcResultsUrl, subMessage)
                }
                else if (isFinished) {
                    state = 3

                    setPRStatus(context, resultValue, mcResultsUrl, subMessage)
                    return true
                }
                return false
            }
        }
    }
    stage ('Execute Tests') {
        parallel helixRunTasks
    }
}