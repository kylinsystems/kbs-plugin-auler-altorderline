# KBS Plugin : Auler Alt Orderline

The fork from https://bitbucket.org/red1/alt.orderline, and converted to be installed via **KBS ObjectData Tool** 

Refer to https://wiki.idempiere.org/en/Plugin:_Alt_OrderLine

## How to install

1. Install **KBS ObjectData Tool** (refer to http://wiki.idempiere.org/en/Plugin:_ObjectDataTool)

2. Install the plugin via Apache Felix Web Console

## How to use


## Fixing

Refine Alt_OrderLine.zip, and re-zip as 2Pack.zip

Add AltOrderLineView.sql in ODT Package (ObjectData **AD_Tab**), since this view will link to new table which created in ODT ObjectData **AD_Table**

## TODO

Reivew Standard PrintFormat **Order_Header AS** for Alt_OrderLine (not packed in ODT)

Review Standard PrintFormat **Order_LineTax AS** for Alt_OrderLine (not packed in ODT)
 * Main PrintFormat(AD_PrintFormat_ID=101) : change **AD_Table** from **C_Order_LineTax_v** to **C_ALTOrder_LineTax_v**
 * Sub FormatItem(AD_PrintFormatItem_ID=152) : change **Name** from **List Prive** to **List Price** 
 * Sub FormatItem(AD_PrintFormatItem_ID=152) : change **AD_Column** from **PriceEnteredList_List** to **PriceList_List** 

**How to create AltOrder_LineView**: Note that the Alt_Orderline tab in Sales Order can be easily and conveniently replaced by the AltOrder_LineView directly