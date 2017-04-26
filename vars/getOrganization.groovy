/**
  * Retrieves the org associated the repository containing this pipeline.
  * This info is read from the input 'GitOrgName' parameter.  If this is not
  * specified, then asserts.
  * @return Org associated with this pipeline.
  */
def call() {
    def gitOrgName = env["GitOrgName"]
    assert gitOrgName != "" : "Could not find GitOrgName parameter"
    return gitOrgName
}