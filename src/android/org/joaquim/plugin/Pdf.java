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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;


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


                final BufferedImage source = ImageIO.read(Base64.decode(png_base64));
                
                final int color = 0xffffff;
        
                final java.awt.Image imageWithTransparency = makeColorTransparent(source, new Color(color));
        
                final BufferedImage transparentImage = imageToBufferedImage(imageWithTransparency);
        
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
                ImageIO.write(transparentImage, "png", baos);
                byte[] res = baos.toByteArray();

                Image image = Image.getInstance(res);
                image.scalePercent(scale);
                
                //image.setTransparency(new int[] { 0xFF, 0xFF });
                
                //PdfImage stream = new PdfImage(image, "", null);
                //stream.put(new PdfName("ITXT_SpecialId"), new PdfName("123456789"));

                //PdfIndirectObject ref = stamper.getWriter().addToBody(stream);
                //image.setDirectReference(ref.getIndirectReference());

                image.setAbsolutePosition(posX, posY);
                PdfContentByte content = stamper.getOverContent(signaturePage);
                content.addImage(image);

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

    /**
     * Convert Image to BufferedImage.
     *
     * @param image Image to be converted to BufferedImage.
     * @return BufferedImage corresponding to provided Image.
     */
    private static BufferedImage imageToBufferedImage(final java.awt.Image image)
    {
        final BufferedImage bufferedImage =
                new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = bufferedImage.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return bufferedImage;
    }

    /**
     * Make provided image transparent wherever color matches the provided color.
     *
     * @param im BufferedImage whose color will be made transparent.
     * @param color Color in provided image which will be made transparent.
     * @return Image with transparency applied.
     */
    public static java.awt.Image makeColorTransparent(final BufferedImage im, final Color color)
    {
        final ImageFilter filter = new RGBImageFilter()
        {
            // the color we are looking for (white)... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFFFFFFFF;

            public final int filterRGB(final int x, final int y, final int rgb)
            {
                if ((rgb | 0xFF000000) == markerRGB)
                {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                }
                else
                {
                    // nothing to do
                    return rgb;
                }
            }
        };

        final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);

        return Toolkit.getDefaultToolkit().createImage(ip);
    }
}