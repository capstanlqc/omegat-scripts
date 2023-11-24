/* :name = Set Latest Translations :description=
 *
 * @author	Manuel Souto Pico
 * @creation	2023.11.06
 * @version	0.0.1
*/


// path to the folder inside the TM folder
path_to_tmx_dir = "auto/"


// code starts here, do not modify
import javax.swing.JOptionPane;
import org.omegat.util.OConsts;
import org.omegat.gui.main.ProjectUICommands;
import java.nio.file.Files;
import java.nio.file.Path
import java.nio.file.Paths
import groovy.util.*
import java.io.File
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.omegat.util.StaticUtils
import java.nio.file.Path
import org.omegat.util.Preferences


import java.text.SimpleDateFormat
def timestamp = new Date()
def readableSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
def filenameSdf = new SimpleDateFormat("yyyyMMdd_HHmmss")
logDate = readableSdf.format(timestamp)



def get_all_tu_nodes_from_tm_dir(tmx_files_in_dir) {

	def all_tu_nodes = []

	tmx_files_in_dir.each { tmx_file ->
		//@console.println(tmx_file)
		def tmx_content = read_tmx_file(tmx_file)
		def tu_nodes = tmx_content.body.tu.findAll { it }
		all_tu_nodes += tu_nodes
	}

	return all_tu_nodes
}

def get_files_in_dir(dir, ext_re) {

	dir.traverse(maxDepth: 0) { // removed: `type: FILES,`
		// create object of Path
		Path path = Paths.get(it.path)
		// call getFileName() and get FileName as a string
		String asset_name = path.getFileName()
		def is_tm = asset_name =~ ext_re
		isDirectory = Files.isDirectory(path)
		// assert m instanceof Matcher
		if (is_tm) {
			files_in_dir.add(it)

		} else if(isDirectory) {
			get_files_in_dir(it, ext_re)
		}
	}
	return files_in_dir
}

def read_tmx_file(tmx_file) {

	def xmlParser = new XmlParser();
	// def root = new XmlSlurper(false,false).parseText(source)
	xmlParser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
	xmlParser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
	xmlParser.setFeature("http://xml.org/sax/features/namespaces", false)
    xmlParser.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
    // ref: https://stackoverflow.com/a/36538456/2095577
    // doc = xmlParser.parseText(tmx_file)
    // return XmlUtil.serialize(doc)
	return xmlParser.parse(tmx_file);
}

def write_tmx_file(tmx_content, tmx_file) {

	tmx_file.withWriter("UTF-8") { w ->
		new XmlNodePrinter( new PrintWriter( w ) ).with { p ->
			preserveWhitespace = true;
			p.print( tmx_content );
		}
	}
}


def convert_timestamp_to_long_in_nodes(tu_nodes) {
	return tu_nodes.each { it
		// convert string timestamp to long int
		it.tuv[1].@changedate = it.tuv[1].@changedate.replace('T','').replace('Z','L')
	}
}


def get_complete_matches(tmdir_fs) {

	File tmx_dir = new File(tmdir_fs + path_to_tmx_dir)
	def ext_re = ~/tmx$/

	console.println(tmx_dir)

	if (!tmx_dir.exists()) {
		console.println("The folder /tm/${path_to_tmx_dir} does not exist")
		return
	}

	files_in_dir = [] // must be init outside of the function, because the func is recursive
	def tmx_files_in_dir = get_files_in_dir(tmx_dir, ext_re)

	console.println(tmx_files_in_dir)
	console.println("")

	def tu_nodes = get_all_tu_nodes_from_tm_dir(tmx_files_in_dir)
	return convert_timestamp_to_long_in_nodes(tu_nodes)
}


def get_latest_trans(tu_nodes) {
	return tu_nodes.collectEntries { it ->
    	[ (it.tuv[0].seg.text()): get_target_nodes(tu_nodes, it.tuv[0].seg.text()).max{ it.@changedate } ]
	}
}


def get_target_nodes(map, source_text) {
	return map.findAll { it
		it.tuv[0].seg.text() == source_text
	}.collect { it.tuv[1] }
}


def get_tm_entries(tmx_file) {

	def tmx_content = read_tmx_file(tmx_file)
	def tu_nodes = tmx_content.body.tu.findAll { it }

	return convert_timestamp_to_long_in_nodes(tu_nodes)
}


def update_to_latest(internal_entries, external_entries) {
	latest_merged = get_latest_trans(internal_entries + external_entries)
	return latest_merged.findAll { it
		internal_entries.find { it.tuv[0].seg.text() }
	}
}



def gui() {

	def props = project.projectProperties
	if (!props) {
		final def msg   = 'No project opened.'
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
		return
	}

	project_save = new File(props.projectInternal, OConsts.STATUS_EXTENSION)
	tmdir_fs = props.getTMRoot() // fs = forward slash

	// from reference/external TMs
	external_entries = get_complete_matches(tmdir_fs)
	latest_trans = get_latest_trans(external_entries)

	// from project save
	internal_entries = get_tm_entries(project_save)

	// these are the latest entries of all combined translations (form ref as well as working TM)
	latest_internal_entries = update_to_latest(internal_entries, external_entries)

	console.println(latest_internal_entries)

	console.println("These are the entries that need to be registered to project save, together with metadata")
	// project_save.save()

	console.println("Probably some fine -tuning is necessary not to mix default and alternative translations")

}

return