/* :name = MT -- Translate with DeepL API :description=foo
 * 
 * @author      Manuel Souto Pico
 * @date        2024-06-07
 * @version     0.0.3
 */

// https://mvnrepository.com/artifact/com.deepl.api/deepl-java
// https://github.com/DeepLcom/deepl-java
@Grapes(
    @Grab(group='com.deepl.api', module='deepl-java', version='1.5.0')
)

import org.omegat.util.StaticUtils
import org.omegat.util.OConsts
import org.omegat.core.data.PrepareTMXEntry
import org.omegat.core.data.TMXEntry
import com.deepl.api.*;

// constants

def pretrans = true


// functions

def get_api_key() {

    config_dir = StaticUtils.getConfigDir()
    api_key_file = new File(config_dir + File.separator + "keys" + File.separator + "deepl_api_key.txt")
    if (! api_key_file.exists()) {
        console.println("API key file (deepl_api_key.txt) not found in the configuration folder.")
        return
    }
    String api_key = api_key_file.text
    return api_key.trim()
}


def repair_langtags(tags, translator) {

    /* check whether the full tag is supported, if not fetch only language subtag
     * 
     * tags is expected to be a map like this:
     * ["source_lang": "aa-BB", "target_lang": "xx-YY"]
    */
    repaired_tags = [:]

    deepl_source_languages = translator.getSourceLanguages().collect { it.getCode() }
    deepl_target_languages = translator.getTargetLanguages().collect { it.getCode() }

    repaired_tags["source_lang"] = (deepl_source_languages.contains(tags["source_lang"])) ? tags["source_lang"] : tags["source_lang"].split("-")[0]
    repaired_tags["target_lang"] = (deepl_target_languages.contains(tags["target_lang"])) ? tags["target_lang"] : tags["target_lang"].split("-")[0]

    return repaired_tags
}


def get_transl(segm_list, source_language, target_language, options) {
    
    String api_key = get_api_key()
    translator = new Translator(api_key);

    tags = repair_langtags(
        ["source_lang": source_language.toString(), "target_lang": target_language.toString()], 
        translator
        )

    List<TextResult> results = translator.translateText(segm_list, tags["source_lang"], tags["target_lang"], options = null);
    return results.collect { it.getText() }
}


def set_options() {
    new TextTranslationOptions().setFormality(Formality.More)
}

/* demo:
TextResult result = translator.translateText("You are wrong.", "en", "es", $options = null);
console.println(result.getText()); // "Bonjour, le monde !"
*/

def timestamp = new Date().format("YYYYMMddHHmm")
def prop = project.projectProperties
def project_root =  prop.projectRootDir
def source_lang =   prop.getSourceLanguage()
def target_lang =   prop.getTargetLanguage()
def proj_name =     prop.projectName
def tmdir_fs =      prop.getTMRoot() // fs = forward slash
def mt_dpath =  pretrans ? prop.getTMRoot() + "auto" + File.separator + "mt" : prop.getTMRoot() + "mt"
new File(mt_dpath).mkdirs()
def omegat_dir =    prop.projectInternal // same as prop.getProjectInternal()

// def project_save_fobj = new File(prop.projectInternal, OConsts.STATUS_EXTENSION)
def tmxsave = prop.getProjectInternal() + OConsts.STATUS_EXTENSION
def mt_fpath = mt_dpath + File.separator + "deepl_${timestamp}" + OConsts.TMX_EXTENSION;


def segm_list = project.allEntries.collect { it.getSrcText() } // SourceTextEntry
def options = null // set_options() // todo
def translations = get_transl(segm_list, source_lang, target_lang, options)

// backlog: remove entries that already have a translation in the working TM
// backlog: remove entries that already have a 100% match in any ref TM
// backlog: remove entries with low QEst score

project.allEntries.each { ste ->

    def index = ste.entryNum()-1
    def target = translations[index]

    def entry = new PrepareTMXEntry()

    entry.source = ste.getSrcText()
    entry.translation = target
            
    project.setTranslation(ste, entry, true, TMXEntry.ExternalLinked.xAUTO)
    // 3rd parameter indicates whenever we save this entry as a default translation (true) or alternative (false)
    // when true, the first parameter is not really used, if false we use its key to set alternative.
    // Fourth parameter ExternalLinked.xAUTO is used when entries come from tm/autop (nothing to do with machine translation). 
    // There is no equivalent to indicate entry coming from MT because OmegaT does not have a specific color for that.

}
project.ProjectTMX.save(prop, mt_fpath, true)
// 3rd parameter: false is used by OmegaT when there were no modifications since last save. 
// set to true, or if you want to use DeepL only for not yet translated segments, 
// use true only if there was at least one segment translated.

return // to avoid printing the last variable in memory
