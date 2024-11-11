/*:name = Caret at the target end :description = Places caret at the end of the activated segment's target text 
 *
 *  @author:   Kos Ivantsov
 *  @date:     2024-11-11
 *  @version:  0.1
 */

def gui() {
    def target = editor.getCurrentTranslation()
    entryPos = editor.editor.getCaretPosition()
    editor.editor.setCaretPosition(entryPos + target.size())
}
