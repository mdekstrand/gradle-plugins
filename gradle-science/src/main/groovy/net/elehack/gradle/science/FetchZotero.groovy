package net.elehack.gradle.science

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Fetch a Zotero bibliography.
 */
class FetchZotero extends ConventionTask {
    /**
     * Authentication key for the Zotero API.
     */
    def String authKey

    def String zoteroUrl = 'https://api.zotero.org/'
    def int pageSize = 50

    def String collection
    def String user
    def String group

    def output = 'references.bib'

    private Set<String> itemKeyCache

    FetchZotero() {
        onlyIf {
            getAuthKey() != null && !project.gradle.startParameter.offline
        }
        inputs.property 'collectionPath', {
            collectionPath
        }
        inputs.property 'ItemSet', {
            itemKeys
        }
    }

    @OutputFile
    File getOutputFile() {
        project.file(output)
    }

    String getCollectionPath() {
        def sb = new StringBuilder()
        if (user != null) {
            if (group != null) {
                logger.warn 'both user {} and group {} specified, using user', user, group
            }
            sb.append('/users/').append(user)
        } else if (group != null) {
            sb.append('/groups/').append(group)
        } else {
            logger.error 'no user or group specified'
            throw new IllegalStateException('no user or group specified')
        }

        if (collection != null) {
            sb.append('/collections/').append(collection)
        }
        sb.append('/items')
        return sb.toString()
    }

    @TaskAction
    void fetchBibliography() {
        logger.info 'retrieving bibliography'
        logger.info 'we expect it to have {} items', itemKeys.size()
        def entryLine = ~/^@\w+\{/
        outputFile.withWriter { writer ->
            def start = 0
            def lastPage = pageSize
            def http = new HTTPBuilder(zoteroUrl)
            while (lastPage >= pageSize) {
                http.request(GET, TEXT) { req ->
                    uri.path = collectionPath
                    uri.query = [key: authKey, format: 'bibtex',
                            limit: pageSize, start: start]
                    response.success = { resp, reader ->
                        lastPage = 0
                        reader.eachLine { String line ->
                            if (line =~ entryLine) {
                                if (lastPage == 0 && start != 0) {
                                    writer.write(',\n\n')
                                }
                                lastPage += 1
                            }
                            writer.write(line)
                            writer.write('\n')
                        }
                    }
                }
                logger.info 'received page of {} items', lastPage
                start += lastPage
            }
        }

    }

    private Set<String> fetchItemKeys() {
        logger.info 'retrieving item list'
        def http = new HTTPBuilder(zoteroUrl)
        def items = new LinkedHashSet<String>()
        http.request(GET, TEXT) { req ->
            uri.path = collectionPath
            uri.query = [key: authKey, format: 'keys']
            response.success = { resp, reader ->
                reader.eachLine { line ->
                    items << line.trim()
                }
            }
        }
        return items
    }

    public synchronized Set<String> getItemKeys() {
        if (itemKeyCache == null) {
            itemKeyCache = fetchItemKeys()
        }
        return itemKeyCache
    }
}
