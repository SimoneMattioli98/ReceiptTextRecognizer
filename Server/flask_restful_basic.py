from flask import Flask
from flask_restful import Api
from classes.Tesseract import Tesseract
from classes.Images import Images
app = Flask(__name__)
api = Api(app)

api.add_resource(Images, '/image')
api.add_resource(Tesseract, '/tesseract')

if __name__ == '__main__':
    app.run(debug=True)
