A netbeans plugin for perforce

# Introduction #

This plugin was created out desperation for something that would work to provide easy access to checkout and add of files in perforce while I was using netbeans.

# Details #

I started out with a simple concept of just providing the toolbar and context menu functions so that I could easily get access to the p4 command line actions I needed most.  I tried using the VCS stuff, but it was just too hard to make sense of the documentation on line and in source examples.

At the same time, the SVN module was being developed, and I started conversing with some netbeans people on line and at JavaOne that year about making the SVN module actually be the new "VCS" interface so that all the work done there could actually be reused.  This was met with some "yeah we should do that" kinds of responses from some people, but nothing every seemed to have happened.

Certainly, without VCS API integration, renames and other refactorings can be less integrated.  I tend to just use P4V to do those things directly and netbeans reflects what I've done afterward anyways.