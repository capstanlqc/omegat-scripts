# omegat-scripts
Groovy scripts that can run in OmegaT.  
Below you'll find short info on some of the scripts.

## Scripts run by the user

* **[`goto_next_filtered_note.groovy`](goto_next_filtered_note.groovy) / [`goto_prev_filtered_note.groovy`](goto_prev_filtered_note.groovy)**
  <details>
    The scripts will activate the next/previous segment where the note contains text either defined in the file <code>&lt;project_folder&gt;/filtered_note.txt</code>, or, if the file is not found, in the scripts themselves (currently <code>XYZZZ</code>).
  </details>

* **[`goto_next_ta_note.groovy`](goto_next_ta_note.groovy)**
  <details>
    The script will activate the next segment which has a match in the first TMX file found in <code>&lt;project_folder&gt;/notes</code>. Works even without the plugin that shows T&A notes.
  </details>

* **[`utils_alt_with_corrected_table.groovy`](utils_alt_with_corrected_table.groovy)**
  <details>
    This script can create a TMX with alternative translations for segments defined in a table/spreadsheet.  
    The script expects a spreadsheet called `correct` (possible extensions: `tsv`, `xls`, `xlsx`) in the project directory.
    The spreadsheet should contain three columns:
     
    | Segment ID | OmegaT Source Text | Correct Target Text |
    |------------|--------------------|---------------------|

    Column headers should not be there. Other columns will be ignored. The script outputs one or two files into `<project>/script_output`:
    
    1. `<project>_alt.tmx` with alternative translations for the IDs found both in the OmegaT project and in the correct file if the source text is identical in both

    2. `<project>_errors.tsv` listing records in the correct file where the source text is different; and records in the correct file with IDs not found in the OmegaT project.
  </details>

* **[`utils_import_creds.groovy`](utils_import_creds.groovy)**
  <details>
    This scripts adds credentials data from a plain text file to <code>credential.properties</code> in OmegaT config folder. The user selects the file via a file chooser dialog. Once the selected file is imported, its extension changes to <code>.done</code> and such processed file cannot be used again.
    
    The script also checks if the selected file is a binary file, and if it actually contains the expected credentials data. In case a wrong file is selected, the file chooser dialog appears again.  
    To simplify the check for the above conditions, selecting only one file at a time is possible.
  </details>

* **[`utils_report_auto.groovy`](utils_report_auto.groovy)**
  <details>
    Reports how many segments are populated from <code>tm/auto</code> and <code>tm/enforce</code>. Data is output to the console, can be automatically copied as tab separated values to the clipboard, and exported to a tsv file inside the current project. 
  </details>

* **[`utils_plunet_statsCSV.groovy`](utils_plunet_statsCSV.groovy)**
  <details>
    This script runs match statistics for the project and reports it in a CSV file named <code>Statistics_&lt;project_name&gt;_&lt;locale_code&gt;.csv</code> inside the <code>omegat</code> folder of the current project. The CSV file can be imported in Plunet.
  </details>

* **[`write_source_target.groovy`](write_source_target.groovy)**
  <details>
    When run, the script exports the whole project as <code>script_output/source_target.txt</code> inside the current project's root folder
  </details>

## Event: `entry_activated`

These scripts need to be placed into `<scripts>/entry_activated/` where `<scripts>` is the folder where OmegaT expects to find scripts.

* **[`entry_activated/caret_target_end.groovy`](entry_activated/caret_target_end.groovy)**
  <details>
    With this script, text caret is placed at the end of the target text automatically as soon as a new segment is activated. Caret jumps back at the end on save.
  </details>

* **[`entry_activated/indicate_alternative.groovy`](entry_activated/indicate_alternative.groovy)**
  <details>
    With this script, any time a segment containing alternative translation is activated, a box saying "Alternative translation" is shown for a short time right above the segment.
  </details>

  
