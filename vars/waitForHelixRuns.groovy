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
def call (String helixRunsBlob) {
    // Parallel stages that wait for the runs.
    def helixRunTasks = [:]
    
    // State to minimize status updates.
    // 0 = Not yet updated
    // 1 = Pending updated
    // 2 = Started updated
    int state = 0;

    for (int i = 0; i < helixRunsBlob.size(); i++) {
        def currentRun = helixRunsBlob[i];
        def queueId = currentRun['QueueId']
        def correlationId = currentRun['CorrelationId']
        helixRunTasks[queueId] = {

            waitUntil {
                // Check the state against the Helix API
                def response = httpRequest "https://helix.dot.net/api/jobs/${correlationId}"
                def content = JsonSlurper().parseText(response.content)
                boolean isPending = content.WorkItems.Running == 0 && content.WorkItems.Finished == 0
                boolean isFinished = content.WorkItems.Unscheduled == 0 && content.WorkItems.Waiting == 0 && content.WorkItems.Running == 0
                boolean isRunning = !isPending && !isFinished
                content = null

                if (isPending && state == 0) {
                    state = 1
                    setPRStatus(queueId, "PENDING", "", "Waiting")
                }
                else if (isRunning && state < 2) {
                    state = 2
                    setPRStatus(queueId, "PENDING", "https://ci.dot.net", "Started")
                }
                else if (isFinished) {
                    state = 3
                    // Check the results
                    setPRStatus(queueId, "PENDING", "https://ci.dot.net", "Finished")
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