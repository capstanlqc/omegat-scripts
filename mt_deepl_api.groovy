/* :name = MT -- Translate with DeepL API :description=foo
 * 
 * @author      Manuel Souto Pico
 * @date        2024-04-26
 * @version     0.0.1
 */

// https://mvnrepository.com/artifact/com.deepl.api/deepl-java
@Grapes(
    @Grab(group='com.deepl.api', module='deepl-java', version='1.5.0')
)

import org.omegat.util.StaticUtils
import com.deepl.api.*;

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

String authKey = get_api_key()
// console.println("authKey: '" + authKey + "'")

translator = new Translator(authKey);
TextResult result = translator.translateText("Hello, world!", null, "fr");
console.println(result.getText()); // "Bonjour, le monde !"