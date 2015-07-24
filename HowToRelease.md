# Creating Releases #

Running `mvn clean deploy` will deploy a new _snapshot_ release to our [self-hosted maven repistory](http://jetlang.googlecode.com/svn/repo/org/jetlang/). It will deploy a tarball and zip-file containing the source, binaries and javadocs, as well as individual artifacts for Maven users.

To cut an actual release, we can use the [Maven Release Plugin](http://maven.apache.org/plugins/maven-release-plugin/). It will boil the actual process down to:

```
mvn release:prepare && mvn:release:perform
```

That will prompt for updated version numbers, deploy artifacts to the repository, tag subversion, and update the site.

# Deploying Maven Site and Javadocs #

## Prereq ##
> Configure your maven settings file to define your google code credentials. The file lives at **$HOME/.m2/settings.xml**. Ensure it contains the following block:

```
<settings>
  <servers>
    <server>
        <id>google-code</id>
        <username>your-user-id</username>
        <password>your-password</password>
    </server>
  </servers>
</settings>
```


## Deploying ##

Run  `mvn clean site-deploy`.

In the case that there are new files added, it is necessary to set the mime type once. You can use the following shell commands on a checkout of the site:

```
find . -name '*.css' | xargs -n 1 svn propset svn:mime-type text/css
find . -name '*.html' | xargs -n 1 svn propset svn:mime-type text/html
find . -name '*.gif' | xargs -n 1 svn propset svn:mime-type image/gif
find . -name '*.png' | xargs -n 1 svn propset svn:mime-type image/png
```