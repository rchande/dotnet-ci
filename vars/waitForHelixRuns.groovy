import groovy.json.JsonSlurper

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
                boolean isRunning = !isNotStarted && !isPending && !isFinished
                statusContent = null

                // We can also grab the info necessary to construct the link for Mission Control from this API.

                if (isPending && state == 0) {
                    state = 1
                    setPRStatus(context, "PENDING", "", "Waiting")
                }
                else if (isRunning && state < 2) {
                    state = 2
                    setPRStatus(context, "PENDING", detailsUrl, "Started")
                }
                else if (isFinished) {
                    state = 3
                    // Check the results
                    // We check the results by going to the API aggregating by correlation id
                    def resultsUrl = "https://helix.int-dot.net/api/2017-04-14/aggregate/jobs?groupBy=job.name&maxResultSets=1&filter.name=${correlationId}"
                    def resultsResponse = httpRequest resultsUrl
                    def resultsContent = (new JsonSlurper()).parseText(resultsResponse.content)

                    // Some checks                    
                    assert resultsContent.size() == 1 : "No results found for helix results API"
                    assert resultsContent[0].Data != null : "No data found in first result for helix results API"
                    assert resultsContent[0].Data.Name == "xunit" : "Data in results api not xunit format"
                    def passedTests = resultsContent[0].Data.Status.pass
                    def failedTests = resultsContent[0].Data.Status.fail
                    def skippedTests = resultsContent[0].Data.Status.skip
                    def totalTests = passedTests + failedTests + skippedTests

                    def resultValue
                    def subMessage
                    if (failedTests != 0) {
                        resultValue = "FAILURE"
                        subMessage = "Failed ${failedTests}/${totalTests} (${skippedTests} skipped)"
                    }
                    else {
                        resultValue = "SUCCESS"
                        subMessage = "Passed ${passedTests} (${skippedTests} skipped)"
                    }

                    setPRStatus(context, resultValue, detailsUrl, subMessage)
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