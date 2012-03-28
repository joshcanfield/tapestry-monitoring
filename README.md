Tapestry Monitoring
===================

Lightweight monitoring for [Apache Tapestry Web Framework](http://tapestry.apache.org]).

Add @Monitor to any
[IOC Service](http://tapestry.apache.org/defining-tapestry-ioc-services.html)
or [Component](http://tapestry.apache.org/component-classes.html) method and get insight into their usage and
performance.

Download
-------

Add the dependency to your POM:

    <dependency>
        <groupId>com.joshcanfield</groupId>
        <artifactId>tapestry-monitoring</artifactId>
        <version>1.0.1</version>
    </dependency>

Or to your gradle build:

    compile 'com.joshcanfield:tapestry-monitoring:1.0.1'


Example
-------

Need to know how long that database activity is taking?

    package org.example.testapp.pages;
    ...
    public class EditProduct {

        @Monitor
        void loadProduct() { ... }
    }

Pull up your favorite JMX app and look for

    org.example.testapp:package=pages,name=EditProduct,monitor=loadProduct,type=Monitor


See more [Examples](https://github.com/joshcanfield/tapestry-monitoring/wiki/examples)