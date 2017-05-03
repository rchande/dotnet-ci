import org.dotnet.ci.util.Agents

// Example:
//
// simpleNode('OSX10.12', 'latest') { <= braces define the closure, implicitly passed as the last parameter
//     checkout scm
//     sh 'echo Hello world'
// }

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  osName - Docker image to use
//  imageVersion - Version of the OS image.  See Agents.getMachineAffinity
//  body - Closure, see example
def call(String osName, version, Closure body) {
    node (Agents.getAgentLabel(osName, version)) {
        // Wrap in a try finally that cleans up the workspace
        try {
            timestamps {
                // Wrap in the default timeout of 120 mins
                timeout(120) {
                    body()
                }
            }
        }
        finally {
            try {
                // Archive anything in the standard log folder
                archiveLogs()
                // Clean
                step([$class: 'WsCleanup'])
            }
            catch (e) {
                echo "Error during cleanup: ${e}"
            }
        }
    }
}