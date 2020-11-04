import base64
import os
from pathlib import Path
from PIL import Image
import cv2
from flask import request
from flask_restful import Resource
import numpy as np
from libs.preprocessing import otsu_thresholding,denoise_gaussian, denoise_morph_open, get_response_image
from libs.crop_background import cut_dark_background, cut_white_background

dir_name = "images"


# class used for image pre processing

class Images(Resource):

    def post(self):

        # get name and image from request

        image_string = request.form.getlist('image')[0]
        image_name = os.path.basename(request.form.getlist('name')[0])
        name, extension = os.path.splitext(image_name)
        # create directory if doesn't exist and get the path to it
        global dir_name
        Path(dir_name).mkdir(parents=True, exist_ok=True)
        path = Path(__file__).parent.parent / dir_name
        Path(os.path.join(path, name)).mkdir(parents=True, exist_ok=True)
        path = Path(__file__).parent / os.path.join(path, name)

        # save image to folder and convert it to tiff and increase dpi

        path_image = os.path.join(path, image_name)
        f = open(path_image, "wb")
        f.write(base64.decodebytes(image_string.encode()))
        im = Image.open(path_image)
        image_name_tiff = image_name.replace(extension, ".tiff")
        path_image_tiff = os.path.join(path, image_name_tiff)
        im.save(path_image_tiff, jfif_unit=1, dpi=(900, 900))
        os.remove(path_image)
        im_gray = Image.open(path_image_tiff).convert('L')
        im_gray = np.array(im_gray)

        # cut image background and separate the receipt

        denoise_image_1 = denoise_gaussian(im_gray)
        th_image = otsu_thresholding(denoise_image_1)[1]
        denoise_image_2 = denoise_morph_open(th_image)
        im_rgb = cut_white_background(denoise_image_2)

        # save the pre processed image in jpg and return it to the client

        im_rgb = Image.fromarray(im_rgb.astype('uint8'))
        image_name_jpg = image_name_tiff.replace('tiff', 'jpg')
        path_image_jpg = os.path.join(path, image_name_jpg)
        im_rgb.save(path_image_jpg)
        return {'image': get_response_image(path_image_jpg),
                'id': image_name_jpg}
