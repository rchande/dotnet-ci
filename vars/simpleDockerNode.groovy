import org.dotnet.ci.util.Agents

// Runs a set of functionality on the default node
// that supports docker.
def call(String dockerImageName, Closure body) {
    node (Agents.getMachineAffinity('Ubuntu16.04', 'latest-or-auto-docker')) {
        // Wrap in a try finally that cleans up the workspace
        try {
            // Wrap in the default timeout of 120 mins
            timeout(120) {
                docker.image(dockerImageName).inside {
                    body()
                }
            }
        }
        finally {
            step([$class: 'WsCleanup'])
        }
    }
}