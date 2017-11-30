package org.joaquim.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Set;
import java.util.List;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfImage;
import com.itextpdf.text.pdf.PdfIndirectObject;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.codec.Base64;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.Rectangle;

import java.io.ByteArrayOutputStream;

public class Pdf extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        if (action.equals("add_image")) {

            String pdf_base64 = data.getString(0);
            String png_base64 = data.getString(1);
            Integer posX = data.getInt(2);
            Integer posY = data.getInt(3);
            Integer scale = data.getInt(4);

            Integer signaturePage = 1;

            try {
                ByteArrayOutputStream pdf_out = new ByteArrayOutputStream();

                PdfReader reader = new PdfReader(Base64.decode(pdf_base64));
                PdfStamper stamper = new PdfStamper(reader, pdf_out);                

                if ( posX == 0 && posY == 0 ) {
                    
                    AcroFields fields = stamper.getAcroFields();

                    Set<String> fldNames = fields.getFields().keySet();
                    for (String fldName : fldNames) {

                        List<AcroFields.FieldPosition> positions = fields.getFieldPositions(fldName);
                        Rectangle rect = positions.get(0).position; // In points:
                        float left = rect.getLeft();
                        float bTop = rect.getTop();
                        float width = rect.getWidth();
                        float height = rect.getHeight();

                        signaturePage = positions.get(0).page;

                        //System.out.println(" : Page [" + page + "] PosX[" + left + "] PosY[" + bTop + "] Width[" + width + "] Height[" + height + "]\n\n");
                        posX = (int) left;
                        posY = (int) bTop;

                        //fields.removeField(fldName);
                    }
                }

                Image image = Image.getInstance(Base64.decode(png_base64));
                image.scalePercent(scale);
                image.setTransparency(new int[] { 0xFF, 0xFF });
                
                //PdfImage stream = new PdfImage(image, "", null);
                //stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));

                //PdfIndirectObject ref = stamper.getWriter().addToBody(stream);
                //image.setDirectReference(ref.getIndirectReference());

                image.setAbsolutePosition(posX, posY);
                PdfContentByte over = stamper.getOverContent(signaturePage);
                over.addImage(image);

                stamper.close();
                //reader.close();

                String pdfout_base64 = Base64.encodeBytes(pdf_out.toByteArray());
                //pdf_out.close();
                
                callbackContext.success(pdfout_base64);

            } catch (Exception e) {
                callbackContext.error(e.toString());
            }

            return true;
        }
        // get_position
        else if (action.equals("get_position")) {
            String pdf_base64 = data.getString(0);
            Integer posX = 0, posY = 0;
            try {

                PdfReader reader = new PdfReader(Base64.decode(pdf_base64));
                AcroFields fields = reader.getAcroFields();

                Set<String> fldNames = fields.getFields().keySet();
                for (String fldName : fldNames) {

                    List<AcroFields.FieldPosition> positions = fields.getFieldPositions(fldName);
                    Rectangle rect = positions.get(0).position; // In points:
                    float left = rect.getLeft();
                    float bTop = rect.getTop();
                    float width = rect.getWidth();
                    float height = rect.getHeight();

                    int page = positions.get(0).page;

                    //System.out.println(" : Page [" + page + "] PosX[" + left + "] PosY[" + bTop + "] Width[" + width + "] Height[" + height + "]\n\n");
                    posX = (int) left;
                    posY = (int) bTop;

                    //fields.removeField(fldName);
                }

                reader.close();

                callbackContext.success("{\"posX\" : \"" + posX + "\", \"posY\" : \"" + posY + "\"}");

            } catch (Exception e) {
                callbackContext.error(e.toString());
            }

            return true;
        } else {

            return false;

        }
    }
}