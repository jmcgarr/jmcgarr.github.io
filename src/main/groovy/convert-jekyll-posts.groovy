/*
 * I used this groovy script to convert the wordpress to jekyll content.
 */

def htmlFiles = ~/.*\.html/

String targetPath = 'src/jbake/content/blog'
String sourcePath = 'src/old-content/'

new File( targetPath ).mkdir()
new File( sourcePath ).eachFileMatch( htmlFiles ) { file ->

    // header variables
    String title
    String date
    List<String> tags = []
    String status
    String type = 'post'

    // parse the date from filename
    def dateRegex = /20[0-1][0-9]-[0-1][0-9]-[0-3][0-9]/
    date = file.name.find( dateRegex )

    // parse file contents
    boolean insideTags = false
    int headerMarker = 0
    String body = ''
    file.text.eachLine{

        if (it.startsWith('---')) {
            headerMarker++
            return
        }
        if (headerMarker > 1) {
            body += fixFormatting( it )
            body += '\r'
            return
        }

        // grab title
        if (it.startsWith('title: ')) {
            title = it.replaceFirst('title: ', '').replaceAll("! \'",'').replaceAll('\'','').trim()
            insideTags = false
        }

        // grab status
        if (it.startsWith('status: ')) {
            String tempStatus = it.replaceFirst('status: ', '')
            status = tempStatus == 'publish' ? 'published' : tempStatus
            insideTags = false
        }

        // grab tags
        if (it.startsWith('tags:')) {
            insideTags = true
            return
        }

        // grab tags
        if (insideTags) {
            if (it.startsWith('- ')) {
                tags << it.replaceFirst('- ','')
            } else {
                insideTags = false
            }
        }
    }

    String header = """title=$title
date=$date
type=$type
tags=${tags.join(', ')}
status=$status
~~~~~~
"""



    File converted = new File( "$targetPath/${file.name.replace( date, '' ).replaceFirst('-', '')}" )
    converted.text = header
    converted << body
}

String fixFormatting( String line ) {
    String updated = convertYoutubeLinks( line )
    updated
}

String convertYoutubeLinks( String line ) {
    line.replaceAll('\\[youtube ', '<iframe width="640" height="360" src="http:').replaceAll(']', '" frameborder="0" allowfullscreen></iframe>').replaceAll('watch?v=', 'embed/')
}