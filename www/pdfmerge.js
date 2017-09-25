/*global cordova, module*/

module.exports = {
    add_image: function (pdf_file, image, pos_x, pos_y, scale, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Pdf", "add_image", [pdf_file, image, pos_x, pos_y, scale]);
    }
};
