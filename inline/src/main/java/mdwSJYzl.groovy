import com.atlassian.jira.ComponentManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.watchers.IssueWatcherAccessor
import com.atlassian.jira.issue.watchers.WatcherManager
import com.atlassian.jira.issue.worklog.WorklogImpl
import com.atlassian.jira.issue.worklog.WorklogManager
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

finalMessage = ""

def mainMethod() {
    String jqlQuery = ""
//    logImportantMessage "Executing query: <pre>${jqlQuery}</pre>"
    IssueWatcherAccessor iwa = ComponentAccessor.getComponent(IssueWatcherAccessor.class)
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    def watcherManager = ComponentAccessor.getWatcherManager();
    Locale en = new Locale("en")
    def worklogManager = ComponentAccessor.getComponent(WorklogManager)

    
    def issues = findIssues(jqlQuery)
    for (issue in issues) {
        def watchers = iwa.getWatchers(issue, en)
        if (watchers.contains(user)){
            for (watcher in watchers){
                if (watcher != user){
                    watcherManager.stopWatching(watcher,issue);
                }
            }
            def worklog = new WorklogImpl(worklogManager,
                issue,
                null,
                user.getName(),
                issue.summary,
                new Date(),
                null,
                null,
                3600
            )

            worklogManager.create(issue.reporter, worklog, 0L, false)
            issue.timeSpent = issue.timeSpent == null ? 1*3600 : issue.timeSpent + 1*3600
        }
    }

    return finalMessage
}

static def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.issueManager
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
    def searchProvider = ComponentAccessor.getComponent(SearchProvider.class)

    def query = jqlQueryParser.parseQuery(jqlQuery)
    def results = searchProvider.search(query, user, PagerFilter.unlimitedFilter)

    results.issues.collect { issue -> issueManager.getIssueObject(issue.id) }
}

mainMethod()