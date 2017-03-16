import org.dotnet.ci.util.Agents

// Example:
//
// simpleDockerNode('foo:bar') { <= braces define the closure, implicitly passed as the last parameter
//     checkout scm
//     sh 'echo Hello world'
// }



// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  body - Closure, see example
def call(String dockerImageName, Closure body) {
    call(dockerImageName, 'latest', body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  hostVersion - Host VM version.  See Agents.getDockerMachineAffinity for explanation.
//  body - Closure, see example
def call(String dockerImageName, String hostVersion, Closure body) {
    node (Agents.getDockerMachineAffinity('Ubuntu16.04', 'latest-or-auto-docker')) {
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