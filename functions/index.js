const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.removeExpiredListings = functions.pubsub
  .schedule('every 1 hours')
  .timeZone('Asia/Kolkata')
  .onRun(async (context) => {
    const now = admin.firestore.Timestamp.now();

    const snap = await admin.firestore()
      .collection('food_listings')
      .where('status', '==', 'available')
      .where('expiryDate', '<', now)
      .get();

    if (snap.empty) {
      console.log('No expired listings');
      return null;
    }

    const batch = admin.firestore().batch();
    snap.docs.forEach(doc => {
      batch.update(doc.ref, { status: 'expired' });
    });

    await batch.commit();
    console.log(`Expired ${snap.size} listings`);
    return null;
  });