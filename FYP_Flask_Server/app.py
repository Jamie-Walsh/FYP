import base64
from datetime import datetime
import os
import numpy as np
import requests
from flask import Flask, request, render_template, jsonify
import pickle
import cv2
import tensorflow as tf
import boto3
from shoes import ShoeList
from flask_sqlalchemy import SQLAlchemy
import keras.backend.tensorflow_backend as tb

BUCKET_NAME = os.environ['BUCKET_NAME']
MODEL_NAME = os.environ['MODEL_NAME']

IMG_SIZE = int(os.environ['IMG_SIZE'])  # Global image size
S3 = boto3.client('s3', region_name='eu-west-1',
                  aws_access_key_id=os.environ['ACCESS_ID'],
                  aws_secret_access_key=os.environ['ACCESS_KEY'])
app = Flask(__name__)

# Database configuration for shoe details
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///db.sqlite3'
db = SQLAlchemy(app)


class Shoe(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    shoe_name = db.Column(db.String(50))
    shoe_price = db.Column(db.String(10))
    date_created = db.Column(db.DateTime, default=datetime.now)
    record_deleted = db.Column(db.Integer, default=0)



def load_model(key):
    # Load model from S3 bucket
    response = S3.get_object(Bucket=BUCKET_NAME, Key=key)
    # Load pickle model
    model_str = response['Body'].read()  # .decode('utf-8')
    model = pickle.loads(model_str)
    return model

model = load_model(MODEL_NAME)


# Function that sets the required image specifications to suit the model
def prepare(imgFile):
    try:
        # convert numpy array to greyscale image
        img_array = cv2.imdecode(imgFile, cv2.IMREAD_GRAYSCALE)
        img_array = img_array / 255.00  # Normalise the pixel data
        # Resize and reshape the image to suit the model
        new_array = cv2.resize(img_array, (IMG_SIZE, IMG_SIZE))
        reshapedImg = new_array.reshape(-1, IMG_SIZE, IMG_SIZE, 1)
        img = tf.cast(reshapedImg, tf.float32)  # Convert to float32
        return img
    except Exception as e:
        return 'Invalid input', 400
        pass


def getShoeDetails(ShoeName):
    cost = '0'
    params = {
        'api_key': os.environ['API_KEY'],
        'type': 'search',
        'amazon_domain': os.environ['DOMAIN'],
        'search_term': ShoeName,
        'sort_by': 'featured',
        'total_pages': '1'
    }

    # make the http GET request to Rainforest API
    api_result = requests.get(os.environ['API_BASE_URL'], params)
    data = api_result.json()  # Convert request to JSON

    for results in data['search_results']:  # Required values are nested in the 'search_results' key
        # Check if the result contains price details and the correct shoe name
        if ('prices' in results) and (ShoeName.upper() in results['title'].upper()):
            for price in results['prices']:
                # Save the price and currency values to the variable 'cost'
                cost = "{}".format(price['value'])
            break  # Stop loop after first successful occurrence for speed
        break
    return cost


@app.route('/', methods=['GET'])
def index():
    return render_template('index.html')


@app.route('/predict', methods=['POST'])
def classify_shoe():
    if request.method == 'POST':
        fileUploaded = 0
        # Convert the form to a dictionary
        requestData = dict(request.form)
        # The desired information is stored in the 'shoe' variable
        # If this is not present in the POST request, then stop the process
        if 'shoe' not in requestData:
            if 'shoe' in request.files:
                file = request.files['shoe']
                fileUploaded = 1
            else:
                fileUploaded = 0
                return 'No image attached', 400

        if fileUploaded == 1:
            imgstr = file.read()
        else:
            # b64 = requestData['shoe'][0]
            # b64 + ('=' * (-len(b64) % 4))
            # Decode the base64 string
            imgstr = base64.b64decode(requestData['shoe'])
        # convert string data to numpy array
        npimg = np.fromstring(imgstr, np.uint8)
        # Format the image to suit the model
        cleanImg = prepare(npimg)

        #tb._SYMBOLIC_SCOPE.value = True
        # Send the image through the model to be predicted
        prediction = model.predict(cleanImg, steps=1)
        output = ShoeList[np.argmax(prediction)]

        # Use SQLAlchemy to retrieve price faster than the Rainforest API
        # If the shoe exists in the DB, then retrieve the price from there. Otherwise, use Amazon API
        exists = db.session.query(db.exists().where(Shoe.shoe_name == output)).scalar()
        if exists:
            # Query DB where the shoe name is the predicted shoe
            shoe = db.session.query(Shoe).filter_by(shoe_name=output).first()
            price = shoe.shoe_price
        else:
            # Get price from Amazon API
            price = getShoeDetails(output)
            # Add the shoe name and price to the database
            shoe = Shoe(shoe_name=output, shoe_price=price)
            db.session.add(shoe)
            db.session.commit()  # Commit changes

        # Return JSON details
        return jsonify({
            'shoeName': output,
            'shoePrice': price
        })


if __name__ == '__main__':
    app.run(threaded=True)
