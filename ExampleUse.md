# Checkout Action in Editor #
The Checkout action shown in the editor toolbar below will allow you to quickly check out a file for edit.

<img src='http://www.wonderly.org/netbeans/images/Image1.jpg'>
<br>
<br>
<hr><br>
<br>
<br>
<h1>Diff Action in Editor</h1>
The diff action in the editor toolbar provides access to the graphical diff displayed through the p4merge tool.<br>
<br>
<img src='http://www.wonderly.org/netbeans/images/Image2.jpg'>
<br>
<br>
<hr><br>
<br>
<br>
<h1>P4MERGE Shows Diffs</h1>
The p4merge tool shows graphical diffs as depicted below.<br>
<img src='http://www.wonderly.org/netbeans/images/Image9.jpg'>
<br>
<br>
<hr><br>
<br>
<br>
<h1>Submit Action in Editor</h1>
The submit action submits the single file corresponding to the file currently being edited.<br>
<img src='http://www.wonderly.org/netbeans/images/Image3.jpg'>
<br>
<br>
<hr><br>
<br>
<br>
<h1>Add Action in Editor</h1>
The add action will add the currently edited file to perforce.<br>
<img src='http://www.wonderly.org/netbeans/images/Image4.jpg'>
<br>
<br>
<hr><br>
<br>
<br>
<h1>Global Team->Perforce Sub-Menu</h1>
The global Team menu has a Perforce submenu which provides access to the perforce operations on all files and project trees.<br>
<br>
When a project node is selected in the navigation tree, as shown here, the project menu items will be enabled, not the file items.<br>
<table><tr><td valign='top'>
<img src='http://www.wonderly.org/netbeans/images/projectTree.png' />
</td>
<td width='40' />
<td>
<img src='http://www.wonderly.org/netbeans/images/projectMenu.png' />
</td>
</tr>
</table>

When a source tree node is selected in the navigation tree, as shown here, the file based menu items will be enabled, not the project items.<br>
<table><tr><td valign='top'>
<img src='http://www.wonderly.org/netbeans/images/sourceTree.png' />
</td>
<td width='40' />
<td>
<img src='http://www.wonderly.org/netbeans/images/sourceMenu.png' />
</td>
</tr>
</table>
<br>
<br>
<hr><br>
<br>
<br>
<h1>File Context Perforce Menu</h1>
The popup, editor context menu also includes a perforce menu to show the perforce operations there as well.   The toolbar is intended as the most convenient access to the normal checkout, add, diff and submit actions.  The popup context menu also contains a "p4 reopen -t +w" option to convert a file type to writable text when checked in.  This can be useful for some IDE config and other similar type files that you don't always want to have to check out and back in, but want to update them into the depot occasionally as references.<br>
<br>
Typically, we do this with IDE config trees that we want new developers/client depots to have access to, but which are typically customized per developer.