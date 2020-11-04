import collections
from itertools import groupby
import numpy as np
import sys
import cv2
from PIL import ImageDraw

np.set_printoptions(threshold=sys.maxsize)
from libs.preprocessing import edging_preprocess_image, get_contour, adjust_receipt_rotation, get_cropped_image


# find the top and bottom coordinates (x or y) based on the 'y' value
# 'img' -> numpy array of the image
# 'y' -> if true calculates the y top and the y bottom, if false x top (left) and x bottom (right)
def get_top_bot_coord_black(img, y=False):
    if y:
        # print("Y")
        array_c = []
        for array in img:
            array_c.append(collections.Counter(array)[0])

    else:
        # print("X")
        array_c = np.zeros(img.shape[1])
        for i in range(0, img.shape[0]):
            for j in range(0, img.shape[1]):
                if img[i][j] == 0:
                    array_c[j] = array_c[j] + 1

    count_dups = np.array([sum(1 for _ in group) for _, group in groupby(array_c)])
    # print("count dups \n {}".format(count_dups))
    boolean = count_dups > 10
    # new_array = count_dups[boolean]

    new_array = count_dups[count_dups >= 200]

    # print("new array \n {}".format(new_array))
    if len(new_array) == 0:
        if y:
            c_top = 0
            c_bot = img.shape[0]
        else:
            c_top = 0
            c_bot = img.shape[1]
    elif len(new_array) == 1:
        index_neutral, = np.where(count_dups == new_array[0])
        if y:
            c_top, c_bot = one_side_y(index_neutral[0], count_dups, img)
        else:
            c_top, c_bot = one_side_x(index_neutral[0], count_dups, img)
    else:
        higher = -1
        index_start = -1
        index_end = -1
        temp_copy = np.copy(count_dups)
        for i in range(0, len(new_array) - 1):
            index_1, = np.where(temp_copy == new_array[i])
            temp_copy[index_1[0]] = 1
            index_2, = np.where(temp_copy == new_array[i + 1])
            dist = index_2[0] - index_1[0]
            # print("Distance between {} and {} -> {}".format(new_array[i], new_array[i + 1], dist))
            if dist >= higher:
                higher = dist
                index_start = index_1[0]
                index_end = index_2[0]
                # print("new higher -> {}".format(higher))

        receipt_portion = index_end - index_start
        #print("receipt portion -> {}".format(receipt_portion))
        if (receipt_portion < index_start) or (receipt_portion < len(count_dups) - index_end):
            if index_start < len(count_dups) - index_end:
                #print("probably the alst element in new_array is the top coordinate")
                index, = np.where(count_dups == new_array[len(new_array) - 1])
            else:
                #print("probably the first element in new array is the bottom coordinate")
                index, = np.where(count_dups == new_array[0])
            if y:
                c_top, c_bot = one_side_y(index[0], count_dups, img)
            else:
                c_top, c_bot = one_side_x(index[0], count_dups, img)
        else:
            #print("both top and bottom found")

            c_top = sum(count_dups[:index_start + 1]) - 5
            c_bot = sum(count_dups[:index_end - 1]) + 5
    #print("c_top -> {} \n c_bot -> {}".format(c_top, c_bot))
    return c_top, c_bot


def cut_white_background(thresholded_image):
    shape = (0, 0)
    while shape != thresholded_image.shape:
        shape = thresholded_image.shape
        x_top, x_bot = get_top_bot_coord_black(thresholded_image)
        y_top, y_bot = get_top_bot_coord_black(thresholded_image, y=True)
        if x_top < 0:
            x_top = 0
        if y_top < 0:
            y_top = 0
        thresholded_image = thresholded_image[y_top:y_bot, x_top:x_bot]
    return thresholded_image


def one_side_y(index, dups, img):
    if index > len(dups) - index:
        c_top = 0
        c_bot = sum(dups[:index]) + 5

    else:
        c_bot = img.shape[0]
        c_top = y_top = sum(dups[:(index + 1)]) - 5

    return c_top, c_bot


def one_side_x(index, dups, img):
    if index > len(dups) - index:
        c_top = 0
        c_bot = sum(dups[:index]) + 5
    else:
        c_bot = img.shape[1]
        c_top = sum(dups[:(index + 1)]) - 5
    return c_top, c_bot


def cut_dark_background(gray_image, rgb_image):
    canny = edging_preprocess_image(gray_image)

    h_contour = get_contour(canny)

    rgb_image = adjust_receipt_rotation(h_contour, rgb_image)
    gray_image = adjust_receipt_rotation(h_contour, gray_image)

    canny = edging_preprocess_image(gray_image)

    h_contour = get_contour(canny)

    rgb_image = get_cropped_image(rgb_image, h_contour)

    return rgb_image


def get_top_bot_coord_black_split(img, y=False):
    if y:
        #print("Y")
        array_c = []
        for array in img:
            array_c.append(collections.Counter(array)[0])

    else:
        #print("X")
        array_c = np.zeros(img.shape[1])
        for i in range(0, img.shape[0]):
            for j in range(0, img.shape[1]):
                if img[i][j] == 0:
                    array_c[j] = array_c[j] + 1

    count_dups = np.array([sum(1 for _ in group) for _, group in groupby(array_c)])
    # print("count dups \n {}".format(count_dups))

    new_array = count_dups[count_dups >= 70]

    #print("new array \n {}".format(new_array))
    array_tops_bots = []
    if len(new_array) == 0:
        if y:
            c_top = 0
            c_bot = img.shape[0]
        else:
            c_top = 0
            c_bot = img.shape[1]
        array_tops_bots.append([c_top, c_bot])
    else:
        # calcolo le distance tra tutti gli spazi usando gli indici
        # dopo di che potrebbero esserci delle scritte anche prima del primo spazio bianco ( primo elemnto nell'array
        # new array) e anche dopo l'ultimo spazio binaco (ultimo elemento dell'array new array)
        temp_copy = np.copy(count_dups)
        array_coordinates = []
        for i in range(0, len(new_array) - 1):
            index_1, = np.where(temp_copy == new_array[i])
            temp_copy[index_1[0]] = 1
            index_2, = np.where(temp_copy == new_array[i + 1])

            coord = [index_1[0], index_2[0]]
            # facendo questo ho un'array di coordinate che mi identificano da dove inizia ogni possibile parte di testo
            array_coordinates.append(coord)

        index_first_newarr_elem, = np.where(count_dups == new_array[0])
        dist_btw_start_firt_elem = 0 - index_first_newarr_elem[0]
        index_last_newarr_elem, = np.where(count_dups == new_array[len(new_array) - 1])
        dist_btw_end_last_elem = (len(count_dups) - 1) - index_last_newarr_elem

        # se la distanza tha gli indici edll'ultimo spazio binco e la fine dell'array con i duplicati è 0 vuol dire che l'ultimo spazio bianco
        # coincide con la fine del documento
        if dist_btw_end_last_elem != 0:
            coord = [index_last_newarr_elem[0], len(count_dups) - 1]
            array_coordinates.append(coord)
        if dist_btw_start_firt_elem != 0:
            coord = [0, index_first_newarr_elem[0]]
            array_coordinates.insert(0, coord)


        for coordinate in array_coordinates:
            c_top = sum(count_dups[:coordinate[0] + 1]) - 5
            if c_top < 0:
                c_top = 0
            c_bot = sum(count_dups[:coordinate[1] - 1]) + 5
            if c_bot > sum(count_dups):
                c_bot = sum(count_dups)
            array_tops_bots.append([c_top, c_bot])

    return array_tops_bots, len(new_array)


# divide l'immagine in sotto immagini (da pulire il codice)
def crop_image_split(thresholded_image):
    final_result = []
    # si parte con il taglio dall'asse y che torna un array di immagini tagliate per l'asse y e un booleano che
    # sta ad indicare se l'allay ritornato contiene una sola immagine o piu
    array_cropped_images_y, is_image = crop_y_axis(thresholded_image)
    if is_image == 1:
        # se contiene una sola immaigine passa subito al taglio per l'asse x la quale torna un'array di immagini tagliate
        # e un boleano che sta ad indicare se l'array ritornato contiene un'immagine o piu
        array_cropped_images_x, is_image = crop_x_axis(array_cropped_images_y)
        if is_image == 1:
            # se si tratta di un'immagine la inserisco nella codadell'array finale
            final_result.append(array_cropped_images_x[0])
        else:
            # altrimenti ciclo tutte le immagini e le inserisco in coda
            for cropped_image_x in array_cropped_images_x:
                final_result.append(cropped_image_x)
    else:
        # altrimenti ciclo l'array e passo ciascuna immagine al taglio sull'asse x
        for cropped_image_y in array_cropped_images_y:

            array_cropped_images_x, is_image = crop_x_axis(cropped_image_y)
            if is_image == 1:
                final_result.append(array_cropped_images_x[0])

            else:
                for cropped_image_x in array_cropped_images_x:
                    final_result.append(cropped_image_x)

    return final_result


# taglia l'immagine sull'asse y
def crop_y_axis(thresholded_image):
    # prima si elabora l'immagine per osservare i diversi tagli possibili
    array_tops_bots_y, stop = get_top_bot_coord_black_split(thresholded_image, y=True)
    array_return = []
    array_cropped_images = []
    # se vi sono tagli da effettuare e non ci si deve fermare nel taglio (ad esempio perchè l'immagine non varia piu)
    # si ciclano qìle coordinate di taglio
    if len(array_tops_bots_y) != 0 and stop != 0:

        for top_bot in array_tops_bots_y:
            # l'immagine principale viene tagliata in otto immagini e salvate in un array
            cropped_img = thresholded_image[top_bot[0]:top_bot[1], 0:thresholded_image.shape[0]]
            array_cropped_images.append(cropped_img)
        for image in array_cropped_images:

            # ogni immagine tagliata viene poi a sua volta tagliata ulteriormente per verificare se il taglio può essere migliorato
            crop_results, is_image = crop_y_axis(image)
            if is_image:
                array_return.append(crop_results)
            else:
                for result in crop_results:
                    array_return.append(result)
    else:
        # se non vi sono più tagli da effettuare si ritorna l'immagine e un intero che identifica il numero di immagini ritornate
        return thresholded_image, 1
    # se vi sono piu immagini da ritornare si torna un' array con un numero identificante il numero di immagini
    return array_return, len(array_cropped_images)


# simile ad y ma con il taglio per l'asse x
def crop_x_axis(thresholded_image):
    # taglio?
    array_tops_bots_x, stop = get_top_bot_coord_black_split(thresholded_image)
    array_return = []
    array_cropped_images = []

    if len(array_tops_bots_x) != 0 and stop != 0:
        for top_bot in array_tops_bots_x:
            cropped_img = thresholded_image[0:thresholded_image.shape[0], top_bot[0]:top_bot[1]]
            array_cropped_images.append(cropped_img)
        for image in array_cropped_images:
            crop_results, is_image = crop_x_axis(image)
            if is_image:
                array_return.append(crop_results)
            else:
                for result in crop_results:
                    array_return.append(result)
    else:
        return thresholded_image, 1

    return array_return, len(array_cropped_images)
