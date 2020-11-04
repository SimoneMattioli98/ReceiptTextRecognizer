import base64
import io
import numpy as np
from PIL import Image
import cv2

# returns an image where edges are shown
# 'image' -> image we want to find edges of
# return 'edges' -> edged image
def get_edged_image(image, fst_value, snd_value):
    edges = cv2.Canny(image, fst_value, snd_value)
    return edges


# threshold function but the threshold value is calculated by the Otsu function
# 'image' -> image we want to threshold
# return 'ret' -> threshold value used to elaborate the image
# 'threshold_image' -> thresholded image
def otsu_thresholding(image):
    ret, threshold_image = cv2.threshold(image, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
    return ret, threshold_image


def edging_preprocess_image(gray_image):
    ret, th = otsu_thresholding(gray_image)
    canny = get_edged_image(th, 100, 200)
    canny = cv2.morphologyEx(canny, kernel=cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)), op=cv2.MORPH_CLOSE)
    return canny


def get_response_image(image_path):
    image = Image.open(image_path, 'r')
    byte_array = io.BytesIO()
    image.save(byte_array, format='JPEG')
    encoded_image = base64.encodebytes(byte_array.getvalue()).decode('ascii')
    return encoded_image


def denoise_morph_open(image):
    image = cv2.morphologyEx(image, kernel=cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)), op=cv2.MORPH_OPEN)
    return image

# gets the contour with the larger perimeter in the image
# 'image' -> image from where we want to find the contours
# return 'h_contour' -> larger contour
def get_contour(img):
    contours, _ = cv2.findContours(img, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    h_perimeter = 0
    h_contour = []
    for cnt in contours:
        cnt_perimeter = cv2.arcLength(cnt, True)
        if cnt_perimeter > h_perimeter:
            h_perimeter = cnt_perimeter
            h_contour = cnt
    return h_contour


# straightens the receipt if awry
# 'receipt contour' -> receipt's contour
# 'img' -> receipt image we want to straighten up
# return 'rotated' -> rotated image
def adjust_receipt_rotation(receipt_contour, img):
    rect = cv2.minAreaRect(receipt_contour)
    box = cv2.boxPoints(rect)
    box = np.int0(box)
    angle = rect[-1]
    if angle < -45:
        angle = 90 + angle
    (h, w) = img.shape[:2]
    center = (w // 2, h // 2)
    M = cv2.getRotationMatrix2D(center, angle, 1.0)
    rotated = cv2.warpAffine(img, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    return rotated


# crops the image
# 'image' -> the image we want to crop
# 'contour' -> the contour (portion) we want to cut
# return 'cropped' -> cropped image
def get_cropped_image(image, contour):
    x, y, w, h = cv2.boundingRect(contour)
    cropped = image[y:y + h, x:x + w]
    return cropped


# blurs the image to reduce noise using a gaussian kernel
# 'image' -> the image we want to blur
# return 'denoised_image' -> blured image
def denoise_gaussian(image):
    denoised_image = cv2.GaussianBlur(image, (5, 5), 0)
    return denoised_image


# utility function for apply_brightness_contrast()
def empty(a):
    pass


# applys brightness and contrast to an image
# 'image' -> image we want to filter
# 'brightness' -> brightness value
# 'contrast' -> contrast value
# return
# 'buf' -> filtered image
# 'copy' -> 'buf' copy with values typed on the image
def apply_brightness_contrast(input_img, brightness=255, contrast=127):
    brightness = map(brightness, 0, 510, -255, 255)
    contrast = map(contrast, 0, 254, -127, 127)

    if brightness != 0:
        if brightness > 0:
            shadow = brightness
            highlight = 255
        else:
            shadow = 0
            highlight = 255 + brightness
        alpha_b = (highlight - shadow) / 255
        gamma_b = shadow

        buf = cv2.addWeighted(input_img, alpha_b, input_img, 0, gamma_b)
    else:
        buf = input_img.copy()

    if contrast != 0:
        f = float(131 * (contrast + 127)) / (127 * (131 - contrast))
        alpha_c = f
        gamma_c = 127 * (1 - f)

        buf = cv2.addWeighted(buf, alpha_c, buf, 0, gamma_c)

    copy = buf.copy()
    cv2.putText(copy, 'B:{},C:{}'.format(brightness, contrast), (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
    return buf, copy


# utility function for apply_brightness_contrast()
def map(x, in_min, in_max, out_min, out_max):
    return int((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min)


# calls apply_brightness_contrast()
# 'image' -> image we want to filter
# 'bright' -> brightness value
# 'contrast' -> contrast value
# return
# 'effect' -> filtered image
def bright_contrast(image, bright=0, contrast=0):
    effect, copy = apply_brightness_contrast(image, bright, contrast)

    return effect
