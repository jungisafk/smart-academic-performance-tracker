package com.smartacademictracker.data.utils

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Native Android XLSX parser using ZipInputStream and XmlPullParser
 * No external dependencies needed - uses only Android built-in APIs
 * 
 * XLSX files are ZIP archives containing XML files:
 * - xl/worksheets/sheet1.xml contains the cell data
 * - xl/sharedStrings.xml contains shared string values
 */
object NativeXlsxParser {
    
    private const val TAG = "NativeXlsxParser"
    
    /**
     * Parse XLSX file and extract rows as list of string arrays
     * @param inputStream Input stream of the XLSX file
     * @return Result containing list of rows (each row is a list of cell values as strings)
     */
    fun parseXlsx(inputStream: InputStream): Result<List<List<String>>> {
        return try {
            Log.d(TAG, "Starting XLSX parsing")
            
            // Step 1: Extract shared strings (if any)
            val sharedStrings = extractSharedStrings(inputStream)
            Log.d(TAG, "Found ${sharedStrings.size} shared strings")
            
            // Step 2: Parse worksheet data
            // We need to re-open the stream since we consumed it reading shared strings
            val rows = parseWorksheet(inputStream, sharedStrings)
            Log.d(TAG, "Parsed ${rows.size} rows from worksheet")
            
            Result.success(rows)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XLSX file", e)
            Result.failure(e)
        }
    }
    
    /**
     * Extract shared strings from xl/sharedStrings.xml
     * Returns a map of string ID to string value
     */
    private fun extractSharedStrings(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    Log.d(TAG, "Found sharedStrings.xml")
                    parseSharedStringsXml(zipInputStream, strings)
                    break
                }
                entry = zipInputStream.nextEntry
            }
            
            zipInputStream.close()
        } catch (e: Exception) {
            Log.w(TAG, "No shared strings found or error reading them: ${e.message}")
        }
        
        return strings
    }
    
    /**
     * Parse the sharedStrings.xml file
     */
    private fun parseSharedStringsXml(inputStream: InputStream, strings: MutableList<String>) {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var currentText = StringBuilder()
            var inSi = false // <si> tag contains a string item
            var inT = false  // <t> tag contains the actual text
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "si" -> {
                                inSi = true
                                currentText.clear()
                            }
                            "t" -> {
                                inT = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT && inSi) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "t" -> {
                                inT = false
                            }
                            "si" -> {
                                strings.add(currentText.toString())
                                inSi = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sharedStrings.xml", e)
        }
    }
    
    /**
     * Parse the main worksheet (xl/worksheets/sheet1.xml)
     */
    private fun parseWorksheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            
            while (entry != null) {
                if (entry.name == "xl/worksheets/sheet1.xml") {
                    Log.d(TAG, "Found sheet1.xml")
                    parseWorksheetXml(zipInputStream, sharedStrings, rows)
                    break
                }
                entry = zipInputStream.nextEntry
            }
            
            zipInputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading worksheet", e)
            throw e
        }
        
        return rows
    }
    
    /**
     * Parse the worksheet XML and extract cell data
     */
    private fun parseWorksheetXml(
        inputStream: InputStream,
        sharedStrings: List<String>,
        rows: MutableList<List<String>>
    ) {
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var currentRow = mutableListOf<String>()
            var currentCellValue = StringBuilder()
            var cellType: String? = null // "s" = shared string, "n" = number, etc.
            var inV = false // <v> tag contains the cell value
            var currentRowIndex = -1
            var currentColIndex = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "row" -> {
                                // Start a new row
                                val rowAttr = parser.getAttributeValue(null, "r")
                                currentRowIndex = rowAttr?.toIntOrNull() ?: (currentRowIndex + 1)
                                currentRow = mutableListOf()
                                currentColIndex = 0
                            }
                            "c" -> {
                                // Start a new cell
                                cellType = parser.getAttributeValue(null, "t")
                                currentCellValue.clear()
                                
                                // Get cell reference (e.g., "A1", "B2") to handle empty cells
                                val cellRef = parser.getAttributeValue(null, "r")
                                if (cellRef != null) {
                                    val expectedColIndex = cellRefToColumnIndex(cellRef)
                                    // Fill in empty cells if needed
                                    while (currentColIndex < expectedColIndex) {
                                        currentRow.add("")
                                        currentColIndex++
                                    }
                                }
                            }
                            "v" -> {
                                // Value tag - contains the actual cell value
                                inV = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV) {
                            currentCellValue.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "v" -> {
                                inV = false
                            }
                            "c" -> {
                                // End of cell - process the value
                                val value = when (cellType) {
                                    "s" -> {
                                        // Shared string - look up in sharedStrings list
                                        val stringIndex = currentCellValue.toString().toIntOrNull()
                                        if (stringIndex != null && stringIndex < sharedStrings.size) {
                                            sharedStrings[stringIndex]
                                        } else {
                                            currentCellValue.toString()
                                        }
                                    }
                                    "n", null -> {
                                        // Number or default
                                        val numValue = currentCellValue.toString().toDoubleOrNull()
                                        if (numValue != null && numValue == numValue.toInt().toDouble()) {
                                            numValue.toInt().toString()
                                        } else {
                                            currentCellValue.toString()
                                        }
                                    }
                                    "b" -> {
                                        // Boolean
                                        when (currentCellValue.toString()) {
                                            "1" -> "true"
                                            "0" -> "false"
                                            else -> currentCellValue.toString()
                                        }
                                    }
                                    else -> currentCellValue.toString()
                                }
                                currentRow.add(value)
                                currentColIndex++
                            }
                            "row" -> {
                                // End of row - add to rows list
                                if (currentRow.isNotEmpty()) {
                                    rows.add(currentRow.toList())
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "XML parsing error", e)
            throw IOException("Failed to parse worksheet XML: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing worksheet XML", e)
            throw e
        }
    }
    
    /**
     * Convert cell reference (e.g., "A1", "B2", "AA10") to column index
     * A=0, B=1, Z=25, AA=26, etc.
     */
    private fun cellRefToColumnIndex(cellRef: String): Int {
        var colIndex = 0
        for (char in cellRef) {
            if (char.isLetter()) {
                colIndex = colIndex * 26 + (char.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return colIndex - 1 // Convert to 0-based index
    }
}

