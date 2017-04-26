/**
  * Retrieves the project associated with this pipeline.
  * This info is read from the input 'GitProjectName' parameter.  If this is not
  * specified, then asserts.
  * @return Repository associated with this run.
  */
def call() {
    def gitProjectName = env["GitProjectName"]
    assert gitProjectName != "" : "Could not find GitProjectName parameter"
    return gitProjectName
}