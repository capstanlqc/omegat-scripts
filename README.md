# omegat-scripts
Groovy scripts that can run in OmegaT


## `utils_report_auto.groovy` 
<details>
  Reports how many segments are populated from `tm/auto` and `tm/enforce`. Data is output to the console, can be automatically copied as tab separated values to the clipboard, and exported to a tsv file inside the current project. 
</details>

## `utils_import_creds.groovy`
<details>
  This scripts adds credentials data from a plain text file to `credential.properties` in OmegaT config folder. The user selects the file via a file chooser dialog. Once the selected file is imported, its extension changes to `.done` and such processed file cannot be used again.
  
  The script also checks if the selected file is a binary file, and if it actually contains the expected credentials data. In case a wrong file is selected, the file chooser dialog appears again.  
  To simplify the check for the above conditions, selecting only one file at a time is possible.
</details>
