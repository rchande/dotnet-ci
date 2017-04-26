import hudson.model.*
import jenkins.model.*
import com.microsoft.azure.vmagent.AzureVMCloud
import com.microsoft.azure.vmagent.util.AzureUtil
import com.microsoft.azure.util.AzureCredentials
import com.cloudbees.plugins.credentials.CredentialsMatchers
import com.cloudbees.plugins.credentials.CredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainRequirement

// This script relies on the Azure VM Agents plugin 0.4.5 or later.
// Expected incoming parameters:
// CloudSubscriptionCredentialsId - Id of credentials that drive this cloud. Serves as a lookup for the subscription id 
//                                  so that the cloud name can be located.
// VmTemplateDeclarations - File, relative to root of dotnet-ci repo containing the list of vm templates
// TestOnly (boolean) - If true, then this attempts to do everything up to adding the actual images to the cloud.
//                      This allows for testing of image changes, printing what would be done.
def CloudSubscriptionCredentialsId = build.buildVariableResolver.resolve("CloudSubscriptionCredentialsId")
def VmTemplateDeclarations = build.buildVariableResolver.resolve("VmTemplateDeclarations")
def TestOnly = build.buildVariableResolver.resolve("TestOnly")

// First let's do some basic processing and checks.  The general rule for this script is that if there are 
// any errors, we bail out before clearing the existing templates.  This means that even if the input list gets
// screwed up, we don't mess up the existing template config

// Get the cloud name and find the cloud itself
AzureVMCloud cloud = getCloud(CloudSubscriptionCredentialsId)

// Read the file incoming
streamFileFromWorkspace(VmTemplateDeclarations).eachLine { line ->
    // Skip comment lines
    boolean skip = (line ==~ / *#.*/);
    line.trim()
    skip |= (line == '')
    if (skip) {
        // Return from closure
        return;
    }
}

def AzureVMCloud getCloud(String credentialsId) {
    AzureVMCloud cloud = Jenkins.getInstance().getCloud(getCloudName(credentialsId))

    assert cloud != null : "Could not find cloud specified by credentials id ${credentialsId}"
}

def String getCloudName(String credentialsId) {
    AzureCredentials creds = CredentialsMatchers.firstOrNull(CredentialsProvider.lookupCredentials(
        AzureCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
            Collections.<DomainRequirement>emptyList()),
        CredentialsMatchers.withId(credentialsId));

    if (creds == null) {
        throw new Exception("Could not find credentials with id: " + credentialsId)
    }

    // Otherwise, return the cloud name, which we pull using internal APIs of the cloud
    String cloudName = AzureUtil.getCloudName(credentials.getSubscriptionId())

    assert cloudName != null && cloudName != "" : "Cloud name not valid"
}