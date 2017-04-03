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
                def detailsUrl = "https://helix.dot.net/api/jobs/${correlationId}/details"
                def response = httpRequest detailsUrl
                def content = (new JsonSlurper()).parseText(response.content)

                // If the job info hasn't been propagated to the helix api, then we need to wait around.
                boolean isNotStarted = content.JobList == null
                boolean isPending = !isNotStarted && content.WorkItems.Running == 0 && content.WorkItems.Finished == 0
                boolean isFinished = !isNotStarted && content.WorkItems.Unscheduled == 0 && content.WorkItems.Waiting == 0 && content.WorkItems.Running == 0
                boolean isRunning = !isNotStarted && !isPending && !isFinished
                content = null

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
                    setPRStatus(context, "SUCCESS", detailsUrl, "Finished")
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