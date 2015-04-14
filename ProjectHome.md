This project is a netbeans plug-in that provides perforce access within netbeans.  It does not use the provided SCM support in netbeans, but instead provides access to the perforce actions on individual files and group of files in projects or folders.

This is intended to be a simple tool for simple needs.  It does not use the VCS infrastructure at this time, so you get to click "check out/edit" when you need access to a file and do other things like delete, revert and submit.  I prefer this explicit involvement in the life cycle of my source files so that I don't inadvertently make changes to files.

It could be better to use the VCS module infrastructure for refactoring support, but I find that I can do the things I need done within p4v and that makes it more obvious and easier to manage over all, for me.

If you are interested in participating in the development of this module, or have fixes or enhancements, please open an issue and insert diffs, and/or send me an email about becoming a commit-er if that's appropriate.