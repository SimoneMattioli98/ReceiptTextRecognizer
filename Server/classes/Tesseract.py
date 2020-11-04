import os
from pathlib import Path

import cv2
import imutils
from PIL import Image, ImageEnhance
from flask import request
from flask_restful import Resource
import numpy as np

import pytesseract
from pytesseract import Output

from libs.preprocessing import denoise_gaussian, otsu_thresholding, get_response_image

# directory where images are saved
dir_name = "images"


# class used for text recognition
class Tesseract(Resource):
    def post(self):

        # get the image based on the id

        id_image = request.form.getlist('id')[0]
        name, extension = os.path.splitext(id_image)
        global dir_name
        path_image = Path(__file__).parent.parent / os.path.join(dir_name, name, id_image)
        im = Image.open(path_image)
        name, extension = os.path.splitext(id_image)

        # convert the image to tiff and increase the dpi

        path_image_tiff = str(path_image).replace(extension, ".tiff")
        im.save(path_image_tiff, jfif_unit=1, dpi=(900, 900))
        im = Image.open(path_image_tiff).convert('L')

        # pre process the image and then it is passed to tesseract

        contrast_enhancer = ImageEnhance.Contrast(im)
        factor = 10
        im = contrast_enhancer.enhance(factor)
        bright_enhancer = ImageEnhance.Brightness(im)
        im = bright_enhancer.enhance(factor)

        im = np.array(im)
        height = im.shape[0] * 1.5
        im = imutils.resize(im, height=int(height))

        print(im.shape)

        dictionary = pytesseract.image_to_data(im, output_type=Output.DICT, lang="ita", config="--psm 6")
        n_boxes = len(dictionary['text'])
        string = ''
        content = ''
        current_line = -1

        for i in range(n_boxes):
            if int(dictionary['conf'][i]) > 0:

                if dictionary['line_num'][i] != current_line:
                    current_line = dictionary['line_num'][i]
                    content = content + string + "\n"
                    string = ''
                string = "{} {}".format(string, dictionary['text'][i])
        string = content

        # print tesseract result
        print(string)
        th = Image.fromarray(im.astype('uint8'))
        path_image_jpg = path_image_tiff.replace('tiff', 'jpg')
        th.save(path_image_jpg)
        return {'text': string}
