/*:name = Indicate alternative translation :description = A short flash in the activated segment with alt translation
 *
 *  @author:   Kos Ivantsov (with parts borrowed from Yu Tang)
 *  @date:     2024-11-11
 *  @version:  0.1
 */

import javax.swing.*
import java.awt.*
import java.awt.Font
import javax.swing.border.EmptyBorder
import javax.swing.text.StyleConstants
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyledDocument
import javax.swing.Timer
import javax.swing.SwingUtilities

import org.omegat.core.Core
import org.omegat.gui.editor.SegmentBuilder
import org.omegat.util.gui.Styles
import org.omegat.util.OStrings

def gui() {
    def ste = editor.getCurrentEntry()
    def info = project.getTranslationInfo(ste)
    if (info.defaultTranslation.toString() == "false") {
        def sourceSegmentRect = getSourceSegmentRect()
        DialogManager.showTemporaryDialog("Alternative translation", sourceSegmentRect)
    }
}

class DialogManager {
    static void showTemporaryDialog(String message, Rectangle bounds, int durationMillis = 600) {
        SwingUtilities.invokeLater {
            def dialog = createDialog(message)
            dialog.setBounds(bounds)  // Set the dialog's bounds to match the source segment rectangle

            // Timer to close the dialog after the specified duration
            Timer timer = new Timer(durationMillis, { dialog.dispose() })
            timer.setRepeats(false)
            timer.start()

            dialog.setVisible(true)
        }
    }

    static JDialog createDialog(String text) {
        def dialog = new JDialog()
        dialog.setUndecorated(true)
        dialog.rootPane.setWindowDecorationStyle(JRootPane.NONE)  // No window decoration for a cleaner look

        def pane = createEditorPane(text)
        dialog.add(pane)
        dialog.pack()

        return dialog
    }

    static JEditorPane createEditorPane(String text) {
        def pane = new JTextPane()
        pane.text = text
        pane.with {
            setEditable(false)
            caret.visible = false
            setDragEnabled(true)
            setFont(new Font(Core.getEditor().font.family, Font.ITALIC, Core.getEditor().font.size - 3))
            setForeground(Styles.EditorColor.COLOR_ACTIVE_SOURCE_FG.color)
            setCaretColor(Styles.EditorColor.COLOR_ACTIVE_SOURCE_FG.color)
            setBackground(Styles.EditorColor.COLOR_ACTIVE_SOURCE.color)
            setCaretPosition 0
            setBorder(new EmptyBorder(0, 6, 0, 6))
            
            StyledDocument doc = pane.getStyledDocument()
            SimpleAttributeSet center = new SimpleAttributeSet()
            StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER)
            doc.setParagraphAttributes(0, doc.length, center, false)
        }
        return pane
    }
}

// Method to get the bounding rectangle of the current source segment
Rectangle getSourceSegmentRect() {
    def editor = Core.getEditor()  // Get the OmegaT editor instance
    int activeSegment = editor.displayedEntryIndex  // Index of the current segment

    // Get the viewport and view rectangle for the currently visible area
    def viewport = editor.scrollPane.viewport
    def viewRect = viewport.viewRect

    // Get the segment builder for the active segment
    def sb = editor.m_docSegList[activeSegment]

    // Get the starting position of the source segment
    int startSourcePosition = sb.startSourcePosition
    Point sourceLocation = editor.editor.modelToView(startSourcePosition).location

    // Ensure the source location is within the viewable area
    if (!viewRect.contains(sourceLocation)) {  
        throw new RuntimeException("Source segment must be viewable")
    }

    // Calculate the height of a single line
    FontMetrics fm = editor.editor.getFontMetrics(editor.editor.font)
    int lineHeight = fm.getHeight()

    // Calculate the rectangle based on source position and line height
    int x = (int) viewRect.x
    int y = (int) sourceLocation.y
    int width = (int) viewRect.width
    int height = lineHeight
    Point point = new Point(x, y)
    SwingUtilities.convertPointToScreen(point, viewport.view)  // Convert to screen coordinates

    return new Rectangle((int) point.x, (int) point.y - height, width, height)  // Convert x and y to int
}
