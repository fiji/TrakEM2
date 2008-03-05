/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2005,2006 Albert Cardona and Rodney Douglas.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ini.trakem2.tree;


import ini.trakem2.utils.Utils;
import ini.trakem2.persistence.DBObject;
import ini.trakem2.Project;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;

public class TemplateThing extends DBObject implements Thing {

	private String type;
	private TemplateThing parent = null;
	private ArrayList al_children = null;
	private Hashtable ht_attributes = null;
	/** The string or numeric value, if any, contained in the XML file between the opening and closing tags. */
	private String value = null;

	/** Create a new non-database-stored TemplateThing. */
	public TemplateThing(String type) {
		super(null, -1);
		this.type = type;
	}

	/** Create a new database-stored TemplateThing. */
	public TemplateThing(String type, Project project) {
		super(project); // gets an automatically assigned id
		this.type = type;
	}

	/** Reconstruct a TemplateThing from the database. */
	public TemplateThing(String type, Project project, long id) {
		super(project, id);
		this.type = type;
	}

	/** For reconstruction purposes. */
	public void setup(ArrayList al_children, Hashtable ht_attributes) {
		if (null == al_children || 0 == al_children.size()) {
			this.al_children = null;
		} else {
			this.al_children = al_children;
			//set parent
			for (Iterator it = al_children.iterator(); it.hasNext(); ) {
				TemplateThing child = (TemplateThing)it.next();
				child.parent = this;
			}
		}
		if (null == ht_attributes || ht_attributes.isEmpty()) {
			this.ht_attributes = null;
		} else {
			this.ht_attributes = ht_attributes;
		}
	}

	/** Recursive into children! Will add the attributes as well and grab an id for this instance. */
	public void addToDatabase(Project project) {
		this.project = project;
		this.id = project.getLoader().getNextId();
		super.addToDatabase();
		if (null != ht_attributes && !ht_attributes.isEmpty()) {
			for (Enumeration e = ht_attributes.keys(); e.hasMoreElements(); ) {
				TemplateAttribute ta = (TemplateAttribute)ht_attributes.get(e.nextElement());
				ta.addToDatabase(project);
			}
		}
		if (null == al_children || al_children.isEmpty()) return;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing child = (TemplateThing)it.next();
			child.addToDatabase(project);
		}
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setParent(Thing parent) {
		this.parent = (TemplateThing)parent;
	}

	public Thing getParent() {
		return parent;
	}

	public String toString() {
		return type;
	}

	public String getTitle() {
		return type;
	}

	public String getType() {
		return type;
	}

	public TemplateThing getChildTemplate(String type) {
		if (null == al_children) return null;
		Iterator it = al_children.iterator();
		while (it.hasNext()) {
			TemplateThing child = (TemplateThing)it.next();
			if (child.type.equals(type)) {
				return child;
			}
		}
		return null;
	}

	public boolean addChild(Thing child) {
		if (null == child) return false;
		if (null == al_children) al_children = new ArrayList();
		al_children.add(child);
		child.setParent(this);
		return true;
	}

	public ArrayList getChildren() {
		return al_children;
	}

	public boolean canHaveAsChild(Thing thing) {
		if (null == thing || null == al_children) return false;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			if (tt.type.equals(thing.getType())) {
				return true;
			}
		}
		return false;
	}

	/** Check if a unique path exists through the child trees until reaching a Thing of the given type. Does not look inside that found thing if any. */
	public boolean uniquePathExists(String type) {
		if (null == al_children) return false;
		ArrayList al = scanChildTrees(type, new ArrayList(), new HashSet());
		return 1 == al.size();
	}

	// recursive
	private ArrayList scanChildTrees(String type, ArrayList al, HashSet hs_done) {
		if (null == al) al = new ArrayList();
		if (null == al_children) return al;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			if (tt.type.equals(type)) {
				al.add(tt);
				// don't look any further down for this found Thing
			} else {
				if (!hs_done.contains(tt)) { //don't recurse into TemplateThing instances that have been visited already (TODO future: this could be a limitation that may have to be addressed)
					hs_done.add(tt); //important! Before!
					al = tt.scanChildTrees(type, al, hs_done);
				}
			}
		}
		return al;
	}

	/** Returns the list of parents to reach a particular child, starting at this, and including the child. Only the first path is reported, others are ignored. */
	protected ArrayList getTemplatePathTo(String type, ArrayList al) {
		al.add(this);
		if (null == al_children) return al;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			if (tt.type.equals(type)) {
				// end. Return the list of parents to get here, plus the found type at the end as a means of signal
				//Utils.log2("found " + tt);
				al.add(tt);
				return al;
			} else {
				// scan its children, if any
				//Utils.log2("looking at " + tt);
				ArrayList al2 = tt.getTemplatePathTo(type, (ArrayList)al.clone());
		/*
		//debug:
		String all = "";
		for (int i=0; i<al2.size(); i++) all += " " + al2.get(i);
		Utils.log2("al2: " + all);
		*/
				if (al2.size() > 0 && ((TemplateThing)al2.get(al2.size() -1)).type.equals(type)) {
					return al2;
				}
			}
		}
		return al;
	}

	public boolean addAttribute(String title, Object contents) {
		if (null == title/* || null == contents*/) return false;
		if (title.equals("id")) return true; // no need to store the id as an attribute (but will exists as such in the XML file)
		if (null == ht_attributes) ht_attributes = new Hashtable();
		if (null == ht_attributes.get(title)) {
			ht_attributes.put(title, new TemplateAttribute(title, contents));
			return true;
		} else {
			Utils.log("TemplateThing.addAttribute: I already have such an attribute.");
			return false;
		}
	}

	public Hashtable getAttributes() {
		return ht_attributes;
	}

	public boolean canHaveAsAttribute(String type) {
		if (null == type) return false;
		if (null == ht_attributes) return false;
		if (ht_attributes.containsKey(type)) {
			return true;
		}
		return false;
	}

	/** Returns null always, for TemplateThings don't hold any real object. */
	public Object getObject() {
		return null;
	}

	/** Ah, we love interfaces don't we. This method returns null. */
	public Thing findChild(Object ob) {
		return null;
	}

	/** Used at startup only to fix the incomplete nested repeated entries, by replacing them with the completed ones. */
	/*
	public void fixNested() {
		// collect all existing TemplateThing instances, to avoid concurrent modifications
		ArrayList al = collectAllChildren(new ArrayList());
		// now start replacing
		Hashtable ht = new Hashtable();
		ht.put(type, this); // unnecessary
		for (Iterator it = al.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			if (ht.containsKey(tt.type)) {
				// a previously created and thus more complete instance has the same type: replace this for the more complete one in the parent of this:
				int i = tt.parent.al_children.indexOf(tt);
				tt.parent.al_children.remove(i);
				tt.parent.al_children.add(i, ht.get(tt.type));
				//Utils.log2("replaced " + tt.type + " " + tt.toString() + " with " + ht.get(tt.type));
			} else {
				// add it
				ht.put(tt.type, tt);
			}
		}
	}
	*/

	/** Recursive into children. The parent of each stored TemplateThing are not meaningful for a tree; only the children are meaningful. */
	public Hashtable getUniqueTypes(Hashtable ht) {
		if (ht.containsKey(this.type)) return ht;
		ht.put(this.type, this);
		if (null == al_children || al_children.isEmpty()) return ht;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			tt.getUniqueTypes(ht);
		}
		return ht;
	}

	/** Recursive into children. */
	public ArrayList collectAllChildren(ArrayList al) {
		if (null == al_children) return al;
		al.addAll(al_children);
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing tt = (TemplateThing)it.next();
			tt.collectAllChildren(al);
		}
		return al;
	}

	/*
	// debug:
	public void printChildren(String indent) {
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			TemplateThing child = (TemplateThing)it.next();
			child.printChildren(indent + "/");
		}
	}
	*/

	/** Change the type to new_name. */
	protected void rename(String new_name) {
		if (null == new_name || 0 == new_name.length() || type.equals(new_name)) return;
		Utils.log("Renaming " + type + "  " + id);
		this.type = new_name;
		updateInDatabase("type");
	}

	public boolean remove(boolean check) {
		if (check) {
			if (!Utils.check("Really delete " + this.toString() + (null == al_children || 0 == al_children.size() ? "" : " and all its children?"))) return false;
		}
		// remove the children, recursively
		if (null != al_children) {
			Object[] children = new Object[al_children.size()];
			al_children.toArray(children); // can't delete directly from the al_children because the child will call removeChild on its parent
			for (int i=0; i<children.length; i++) {
				Object ob = children[i];
				if (ob instanceof DBObject) {
					if (!((DBObject)ob).remove(false)) {
						Utils.showMessage("Deletion incomplete, check database, for child: " + ob.toString());
						return false;
					}
				}
			}
		}
		// remove the attributes
		if (null != ht_attributes) {
			Enumeration e = ht_attributes.keys();
			while (e.hasMoreElements()) {
				if (! ((TemplateAttribute)ht_attributes.get(e.nextElement())).remove(false)) {
					Utils.showMessage("Deletion incomplete at attributes, check database for thing: " + this);
					return false;
				}
			}
		}
		// remove the Thing itself
		if (null != parent && !parent.removeChild(this)) {
			Utils.showMessage("Deletion incomplete, check database, for parent of TemplateThing id=" + id);
			return false;
		}
		return removeFromDatabase();
	}

	public boolean removeChild(TemplateThing child) {
		// check that it is contained here
		if (-1 == al_children.indexOf(child)) {
			Utils.log("TemplateThing.removeChild: child " + child + " not contained in parent " + this);
			return false;
		}
		al_children.remove(child);
		return true;
	}

	/** Recursive into children, find those of the given type that have the same immediate parent type as the given TemplateThing. */
	public HashSet collectSimilarThings(TemplateThing tt, HashSet hs) {
		if (type.equals(tt.type) && parent.getType().equals(tt.getParent().getType())) {
			hs.add(this);
		}
		if (null == al_children || al_children.isEmpty()) return hs;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			hs = ((TemplateThing)it.next()).collectSimilarThings(tt, hs);
		}
		return hs;
	}

	/** Recursive into children, find those of the same type as the given TemplateThing and whose number of children is the same to those of the given TemplateThing (to exclude nested types). */
	public HashSet collectSimilarThings2(TemplateThing tt, HashSet hs) {
		if (type.equals(tt.type) && (al_children == tt.al_children /*if both are null*/ || (null != al_children && null != tt.al_children && al_children.size() == tt.al_children.size()))) hs.add(this);
		if (null == al_children || al_children.isEmpty()) return hs;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			hs = ((TemplateThing)it.next()).collectSimilarThings2(tt, hs);
		}
		return hs;
	}

	/** Find things of the same type, eve if their parents are different, recusively into children. */
	public HashSet collectThingsOfEqualType(TemplateThing tt, HashSet hs) {
		if (type.equals(tt.type)) hs.add(this);
		if (null == al_children || al_children.isEmpty()) return hs;
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			hs = ((TemplateThing)it.next()).collectThingsOfEqualType(tt, hs);
		}
		return hs;
	}

	/** Determine whether this instance is nested inside the tree of an instance of the same type (for example, a neurite_branch inside another neurite_branch)*/
	public boolean isNested() {
		Thing p = this.parent;
		while (null != p) {
			if (this.type.equals(p.getType())) {
				return true; // nested!
			}
			p = p.getParent();
		}
		return false;
	}

	/** Only the header !ELEMENT and !ATTLIST. */
	public void exportDTD(StringBuffer sb_header, HashSet hs, String indent) {
		String tag = type.replace(' ', '_');
		if (hs.contains(tag)) return;
		hs.add(tag);
		sb_header.append(indent).append("<!ELEMENT ").append(tag);
		if (null != al_children && 0 != al_children.size()) {
			sb_header.append(" (");
			int c = 0;
			for (Iterator it = al_children.iterator(); it.hasNext(); ) {
				if (0 != c) sb_header.append(", ");
				c++;
				sb_header.append(((TemplateThing)it.next()).type);
			}
			sb_header.append(")");
		} else {
			sb_header.append(" EMPTY");
		}
		sb_header.append(">\n");
		sb_header.append(indent).append("<!ATTLIST ").append(tag).append(" id NMTOKEN #REQUIRED>\n"); // 'id' exists separate from the other attributes
		if (null != ht_attributes && !ht_attributes.isEmpty() ){
			// the rest of the attributes:
			for (Enumeration e = ht_attributes.keys(); e.hasMoreElements(); ) {
				String key = (String)e.nextElement();
				TemplateAttribute ta = (TemplateAttribute)ht_attributes.get(key);
				sb_header.append("\t<!ATTLIST ").append(tag).append(" ").append(ta.getTitle()).append(" NMTOKEN #REQUIRED>\n");
			}
		}
		// if it's a basic type it can contain a DBObject
		if (project.isBasicType(type)) {
			sb_header.append(indent).append("<!ATTLIST ").append(tag).append(" oid NMTOKEN #REQUIRED>\n");
		}
		// node expanded state
		sb_header.append(indent).append("<!ATTLIST ").append(tag).append(" expanded NMTOKEN #REQUIRED>\n"); // TODO should not say #REQUIRED but optional, in XMLese
		// recurse into children
		if (null != al_children && 0 != al_children.size()) {
			for (Iterator it = al_children.iterator(); it.hasNext(); ) {
				((TemplateThing)it.next()).exportDTD(sb_header, hs, indent);
			}
		}
	}

	public void exportXMLTemplate(StringBuffer sb_header, StringBuffer sb_body, HashSet hs, String indent) {
		// write in opening tag, put in there the attributes (and also to sb_header), then close, then call the children (indented), then closing tag.
		// 0 - ELEMENT and ATTLIST
		boolean write_attr = false;
		if (!hs.contains(type)) {
			hs.add(type);
			write_attr = true;
			sb_header.append("\t<!ELEMENT ").append(type);
			if (null != al_children && 0 != al_children.size()) {
				sb_header.append(" (");
				int c = 0;
				for (Iterator it = al_children.iterator(); it.hasNext(); ) {
					if (0 != c) sb_header.append(", ");
					c++;
					sb_header.append(((TemplateThing)it.next()).type);
				}
				sb_header.append(")");
			} else {
				sb_header.append(" EMPTY");
			}
			sb_header.append(">\n");
			sb_header.append("\t<!ATTLIST ").append(type).append(" id NMTOKEN #REQUIRED>\n");
		}
		// 1 - opening tag with attributes:
		sb_body.append(indent).append("<").append(type).append(" id=\"").append(id).append("\"");
		if (null != ht_attributes && !ht_attributes.isEmpty() ){
			// the rest of the attributes:
			for (Enumeration e = ht_attributes.keys(); e.hasMoreElements(); ) {
				String key = (String)e.nextElement();
				TemplateAttribute ta = (TemplateAttribute)ht_attributes.get(key);
				sb_body.append(" ").append(ta.getTitle()).append("=\"").append(ta.getObject().toString()).append("\"");
				if (write_attr) sb_header.append("\t<!ATTLIST ").append(type).append(" ").append(ta.getTitle()).append(" NMTOKEN #REQUIRED>\n");
			}
		}
		sb_body.append(">\n");
		// 2 - list of children:
		if (null != al_children && 0 != al_children.size()) {
			for (Iterator it = al_children.iterator(); it.hasNext(); ) {
				((TemplateThing)it.next()).exportXMLTemplate(sb_header, sb_body, hs, indent + "\t");
			}
		}
		// 3 - closing tag:
		sb_body.append(indent).append("</").append(type).append(">\n");
	}

	public void debug(String indent) {
		StringBuffer sb_at = new StringBuffer(" (id,"); // 'id' exists regardless
		if (null != ht_attributes) {
			for (Iterator it = ht_attributes.values().iterator(); it.hasNext(); ) {
				TemplateAttribute ta = (TemplateAttribute)it.next();
				sb_at.append(ta.getTitle()).append(",");
			}
		}
		sb_at.append(")");
		System.out.println(indent + this.type + sb_at.toString());
		if (null != al_children) {
			if (isNested()) {
				System.out.println(indent + "-- Nested type.");
				return;
			}
			if (indent.length() > 20) {
				System.out.println("INDENT OVER 20 !");
				return;
			}
			for (Iterator it = al_children.iterator(); it.hasNext(); ) {
				((TemplateThing)it.next()).debug(indent + "\t");
			}
		}
	}

	public boolean isExpanded() {
		return project.getLayerTree().isExpanded(this);
	}

	/** Return information on this node and its object. */
	public String getInfo() {
		return "Template Node: " + type;
	}

	/** Recursive into children: clones the whole tree from this node downward. */
	public TemplateThing clone(final Project pr) {
		final long nid = pr.equals(this.project) ? pr.getLoader().getNextId() : this.id;
		final TemplateThing clone = new TemplateThing(this.type, pr, nid);
		clone.project = pr;
		if (!pr.equals(this.project)) clone.addToDatabase();
		if (null == al_children) return clone;
		// clone attributes
		// TODO
		// clone children
		for (Iterator it = al_children.iterator(); it.hasNext(); ) {
			clone.addChild(((TemplateThing)it.next()).clone(pr));
		}
		return clone;
	}
}
