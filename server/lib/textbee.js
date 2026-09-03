const DEFAULT_BASE_URL = 'https://api.textbee.dev/api/v1';

// Sends a single SMS via TextBee's gateway API, using the spare Android
// phone registered as a device in the TextBee dashboard.
async function sendSms(phone, message) {
  const apiKey = process.env.TEXTBEE_API_KEY;
  const deviceId = process.env.TEXTBEE_DEVICE_ID;
  if (!apiKey || !deviceId) {
    throw new Error('TEXTBEE_API_KEY / TEXTBEE_DEVICE_ID environment variables are not set.');
  }
  const baseUrl = process.env.TEXTBEE_BASE_URL || DEFAULT_BASE_URL;

  const response = await fetch(`${baseUrl}/gateway/send-sms`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': apiKey
    },
    body: JSON.stringify({ recipients: [phone], message, deviceId })
  });

  const responseBody = await response.text().catch(() => '');
  if (!response.ok) {
    throw new Error(`TextBee send-sms failed: ${response.status} ${responseBody}`);
  }
  console.log('textbee send-sms response', response.status, responseBody);
}

module.exports = { sendSms };
