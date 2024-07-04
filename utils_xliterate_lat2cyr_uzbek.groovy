/* :name = Xliterate Uzbek Project (Latin to Cyrillic) :description=Transliterates globally in the project any Latin letter, if found, to its Cyrillic counterpart
 *
 * @author      Manuel Souto Pico, Briac Pilpré, Kos Ivantsov
 * @date        2024-07-03 (based on the script for Serbian)
 * @version     0.1.7 (for Uzbek)
 */

@Grab(group='com.ibm.icu', module='icu4j', version='58.2')
import com.ibm.icu.text.Transliterator
transliteratorId = 'Latin-Cyrillic'
console.println()
def gui(){
    def segm_count = 0
    project.allEntries.each { currSegm ->
        editor.gotoEntry(currSegm.entryNum())
        def target = project.getTranslationInfo(currSegm) ? project.getTranslationInfo(currSegm).translation : null

        if (target == null) {
            console.println("----")
            return 
        }

        def newTarget = refine_xlit(target)
        newTarget = transliterate(transliteratorId, newTarget)
        console.println(currSegm.entryNum() + "\t" + target + " => " + newTarget)
        if (target != newTarget) {
            segm_count++
            editor.replaceEditText(newTarget)
        }
    }
    console.println(segm_count + " segments modified")
}

def refine_xlit(text) {
    text = text.replaceAll(/o[\'‘]/, 'ў')
    text = text.replaceAll(/O[\'‘]/, 'Ў')
    text = text.replaceAll(/g[\'‘]/, 'ғ')
    text = text.replaceAll(/G[\'‘]/, 'Ғ')
    text = text.replaceAll(/ch/, 'ч')
    text = text.replaceAll(/C[hH]/, 'Ч')
    text = text.replaceAll(/sh/, 'ш')
    text = text.replaceAll(/S[hH]/, 'Ш')
    text = text.replaceAll(/ng/, 'нг')
    text = text.replaceAll(/Ng/, 'Нг')
    text = text.replaceAll(/NG/, 'НГ')
    text = text.replaceAll(/[\’\']/, 'ъ')
    text = text.replaceAll(/j/, 'ж')
    text = text.replaceAll(/J/, 'Ж')
    text = text.replaceAll(/q/, 'қ')
    text = text.replaceAll(/Q/, 'Қ')
    text = text.replaceAll(/h/, 'ҳ')
    text = text.replaceAll(/H/, 'Ҳ')
    text = text.replaceAll(/bya/, 'бъя')
    text = text.replaceAll(/bye/, 'бъе')
    text = text.replaceAll(/byo/, 'бъё')
    text = text.replaceAll(/byu/, 'бъю')
    text = text.replaceAll(/(?i)([aeouiyl])ya/, '$1я')
    text = text.replaceAll(/(?i)([aeouiyl])ye/, '$1е')
    text = text.replaceAll(/(?i)([aeouiyl])yo/, '$1ё')
    text = text.replaceAll(/(?i)([aeouiyl])yu/, '$1ю')
    text = text.replaceAll(/\bya/, 'я')
    text = text.replaceAll(/\bY[aA]/, 'Я')
    text = text.replaceAll(/\bye/, 'е')
    text = text.replaceAll(/\bY[eE]/, 'E')
    text = text.replaceAll(/\byo/, 'ё')
    text = text.replaceAll(/\bY[oO]/, 'Ё')
    text = text.replaceAll(/\byu/, 'ю')
    text = text.replaceAll(/\bY[uU]/, 'Ю')
    text = text.replaceAll(/i/, 'и')
    text = text.replaceAll(/I/, 'И')
    text = text.replaceAll(/y/, 'й')
    text = text.replaceAll(/Y/, 'Й')
    text = text.replaceAll(/x/, 'х')
    text = text.replaceAll(/X/, 'Х')
    text = text.replaceAll(/ts/, 'ц')
    text = text.replaceAll(/T[sS]/, 'Ц')
    return text
}

def transliterate(transliterateId, text) {
    def filter = /(\s*<[^>]+>\s*)+/
    def transliterator = Transliterator.getInstance(transliterateId)

    def tags = []
    text.eachMatch(filter) { t -> tags.add(t[0]) }

    def chunks = []
    for (String chunk : text.split(filter)) {
        chunks.add(transliterator.transform(chunk))
    }

    def merge = []

    if (chunks.size == 0 && tags.size > 0) {
        merge.add(tags[0])
    }
    else if (chunks.size > 0) {
        for (def i = 0; i < chunks.size; i++) {
            merge.add(chunks[i] + (tags[i] ?: ''))
        }
    }
    else {
        console.println("** WARN ** [" + text + "]")
        return text
    }

    return merge.join('')
}
