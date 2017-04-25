/**
  * Constructs the HelixSource submission parameter given the current environment.
  * @return ghprbPullId if a PR
  */
def call() {
    if (isPR()) {
        def prId = getPRNumber()
    }
    else {

    }
}