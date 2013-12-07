# Michael's Gradle Plugins

[![Build Status](https://travis-ci.org/elehack/gradle-plugins.png?branch=master)](https://travis-ci.org/elehack/gradle-plugins)

These are some Gradle plugins that I use for different things.

## gradle-jruby

The `gradle-jruby` plugin uses JRuby and Bundler to install and run gems.
Useful to compile SASS style sheets, for example.

`build.gradle`:

~~~groovy
apply plugin: 'jruby'

repositories {
    // so we can find JRuby
    mavenCentral()
}

task compileSass(type: GemExec) {
    script 'sass'
    inputs.file 'example.scss'
    args 'example.scss', 'example.css'
}
~~~

`Gemfile`:

~~~ruby
source "https://rubygems.org"

gem "sass"
~~~

## gradle-science

Tasks for doing scientific publication and data analysis.  A lot of publication-related things.

No documentation yet.