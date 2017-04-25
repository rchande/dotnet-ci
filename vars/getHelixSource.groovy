/**
  * Constructs the HelixSource submission parameter given the current environment.
  * @return Helix source
  */
def call() {
    if (isPR()) {
        def projectName = getProject()
        def prId = getPRNumber()
        return "pr/${projectName}/${prId}"
    }
    else {
        assert false : "nyi"
    }
}