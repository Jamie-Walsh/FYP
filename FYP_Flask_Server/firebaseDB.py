import os
import pyrebase
from datetime import date
import datetime
import schedule
import time

now = datetime.datetime.now()
todayDate = date.today().strftime('%d-%m-%Y')
todayTime = now.strftime("%H:%M")
print(todayTime, todayDate)

config = {
  "apiKey": os.environ['FB_API_KEY'],
  "authDomain": os.environ['FB_AUTH_DOMAIN'],
  "databaseURL": os.environ['FB_DATABASE_URL'],
  "storageBucket": os.environ['FB_STORAGE_BUCKET'],
}

firebase = pyrebase.initialize_app(config)
db = firebase.database()


def UpdateArticles():
    # Article data is hardcoded until an API which meets the requirements can be used.
    ARTICLES = {
        'A1': {'article_title':'adidas Y-3 New Yunu Sneaker',
              'article_body':'Yohji Yamamoto‘s adidas Y-3 has released the Yunu sneaker, a new silhouette developed with adidas Skateboarding. Notable design elements include the signature Y-3 branding found atop of the tongue, as well as the leather trims that outline the curves around the shoe. A black sole unit engineered by adidas Skateboarding adds grip, while a leather lining inside provides the footbed with comfort. Take a look at the adidas Y-3 Yunu in “Black/Cloud White” in the gallery above, and pick up a pair for yourself at retailers such as Foot District for €250 EUR (approx. $272 USD).',
              'article_img_uri':'https://image-cdn.hypb.st/https%3A%2F%2Fhypebeast.com%2Fimage%2F2020%2F04%2Fadidas-skateboarding-y-3-yunu-black-cloud-white-yohji-yamamoto-release-information-1.jpg?q=90&w=1400&cbr=1&fit=max',
              'article_date':'{}'.format(todayDate),
              'article_time':'{}'.format(todayTime)},

        'A2': {'article_title':'New Balance "Greek Gods" Pack',
              'article_body':'DTLR has served up the first look at its exclusive New Balance 997 and 997S models in the upcoming “Greek Gods” pack. The sneakers are coined M997 “Perseus” and M997 “Sport Medusa.” For the original 997 model, New Balance has given it a black and “Cool Grey” colorway that sports iridescent 3M trim on the collar, tongue and around the eyestay. Other notable touches include a grey “N” mid-panel logo that sits atop of a snakeskin “N” outline, and the embroidered branding that appears in red and blue colors to match the tongue’s color-changing nature.',
              'article_img_uri':'https://image-cdn.hypb.st/https%3A%2F%2Fhypebeast.com%2Fimage%2F2020%2F04%2Fdtlr-exclusive-new-balance-997-997s-greek-gods-release-information-first-look-1.jpg?q=90&w=1400&cbr=1&fit=max',
              'article_date':'{}'.format(todayDate),
              'article_time':'{}'.format(todayTime)},

        'A3': {'article_title':'Nikes Air Max 2090 By You',
              'article_body':'Nike has introduced its recently-released Air Max 2090 sneaker into its “By You” customization platform, providing customers with multicolored colorways that they can switch up themselves. There are six bases to choose from, with standout starter colorways including “Magma Orange” and “Off Noir.” From here, you can choose the colors for the base, tongue, heel, toe, overlay, mudguard, backtab, midsole wedge and outsole heel.',
              'article_img_uri':'https://image-cdn.hypb.st/https%3A%2F%2Fhypebeast.com%2Fimage%2F2020%2F04%2Fnike-air-max-2090-by-you-sneaker-customization-colorways-release-information-6.jpg?q=90&w=1090&cbr=1&fit=max',
              'article_date':'{}'.format(todayDate),
              'article_time':'{}'.format(todayTime)},

        'A4': {'article_title':'Air Jordan 6 "DMP" Release',
              'article_body':'Jordan Brand has officially unveiled the Air Jordan 6 “DMP” that is set to release later this month. This Defining Moments Pack-special was last seen in October 2019 with an anticipated drop of January 2020, introducing the sneaker as a homage to the original AJ6 that came as part of the 2006 Air Jordan “DMP” pack alongside an Air Jordan 11. For its 2020 retro, Jordan Brand keeps things as original as can be with an upper sporting the traditional black and gold colorway. Smooth suede is used throughout the upper, with rubber and foam coming together to give the tongue support. Terry cotton has been used for the sock liner, completing the blacked-out look.',
              'article_img_uri':'https://image-cdn.hypb.st/https%3A%2F%2Fhypebeast.com%2Fimage%2F2020%2F04%2Fnike-air-jordan-6-dmp-defining-moments-pack-2020-release-information-1.jpg?q=90&w=1400&cbr=1&fit=max',
              'article_date':'{}'.format(todayDate),
              'article_time':'{}'.format(todayTime)},

        'A5': {'article_title':'Nike Air Max 97 White-Laser Crimson',
              'article_body':'Nike has unveiled the latest iteration of its Air Max 97. This time around, the American footwear giant opted for a crisp “White/Laser Crimson” colorway for the classic sneaker. This rendition follows a clean “White/Ice Blue” from last month. Most of the shoes are dominated by white uppers, complemented by hits of “Smoke Gray” and “Laser Crimson” accents. Starting at the top of the shoe is a bold leather Nike Swoosh logo stamped at the tongue, followed by a splash of fluorescent crimson highlights at the central forefoot. Below the topmost section of the body is a thin black line that separates the white uppers from the darker mudguards — a bright red mini-Swoosh pops from its gray panels. All of these details rest over a full-length Air unit sole, sporting a vivid “Laser Crimson” tone through its transparent unit. The Nike Air Max 97 “White/Laser Crimson” is currently available at atmos’ website for $165 USD.',
              'article_img_uri':'https://image-cdn.hypb.st/https%3A%2F%2Fhypebeast.com%2Fimage%2F2020%2F04%2Fnike-air-max-97-white-laser-crimson-cw5419-100-release-001.jpg?q=90&w=1400&cbr=1&fit=max',
              'article_date':'{}'.format(todayDate),
              'article_time':'{}'.format(todayTime)},
    }

    # Loop through the data, adding each one to the article node in firebase real-time db
    for key, value in ARTICLES.items():
        db.child("articles").child(key).set(value)


# Update the articles at 9am every day
schedule.every().day.at("09:00").do(UpdateArticles)

while True:
    schedule.run_pending()
    # Wait a minute before running the schedule check again
    time.sleep(60)
