package com.example.abhi.androidbraintree;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.PaymentMethodNonce;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1234;
    // in emulator local host 10.0.2.2
    final String API_GET_TOKEN = "http://10.0.2.2/braintree/main.php";
    final String API_CHECK_OUT = "http://10.0.2.2/braintree/checkout.php";

    String token,amount;
    HashMap<String,String> paramsHash;

    Button btn_pay;
    EditText edt_amount;
    LinearLayout group_waiting,group_payment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        group_payment = (LinearLayout)findViewById(R.id.payment_group);
        group_waiting = (LinearLayout)findViewById(R.id.waiting_group);

        btn_pay = (Button)findViewById(R.id.btn_pay);
        edt_amount = (EditText)findViewById(R.id.edt_amount);

        new getToken().execute();

        //Event

        btn_pay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPayment();
            }
        });
    }

    private void submitPayment() {
        DropInRequest dropInRequest = new DropInRequest().clientToken(token);
        startActivityForResult(dropInRequest.getIntent(this),REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE)
        {
            if (requestCode == RESULT_OK)
            {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();
                String strNonce = nonce.getNonce();

                if (!edt_amount.getText().toString().isEmpty())
                {
                    amount = edt_amount.getText().toString();
                    paramsHash = new HashMap<>();
                    paramsHash.put("amount",amount);
                    paramsHash.put("nonce",strNonce);


                    sendPayment();
                }
                else
                {
                    Toast.makeText(this, "please enter valid amount", Toast.LENGTH_SHORT).show();
                }
            }
            else if (resultCode == RESULT_CANCELED)
                Toast.makeText(this, "User Cancel", Toast.LENGTH_SHORT).show();
            else
            {
                Exception exception = (Exception)data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.d("ABHI_ERROR",exception.toString());

            }
        }
    }

    private void sendPayment() {
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, API_CHECK_OUT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.toString().contains("Successful"))
                            Toast.makeText(MainActivity.this, "Transaction Successful", Toast.LENGTH_SHORT).show();
                        else {
                            Toast.makeText(MainActivity.this, "Transaction Failed", Toast.LENGTH_SHORT).show();
                        }
                        Log.d("ABHI_LOG", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ABHI_ERROR", error.toString());
            }
        })

        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if(paramsHash == null)
                    return null;
                Map<String,String> params = new HashMap<>();
                for(String key:paramsHash.keySet())
                {
                    params.put(key,paramsHash.get(key));
                }
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type","application/x-www-form-urlencoded");


                return params;
            }

        };

        queue.add(stringRequest);

    }

    private class getToken extends AsyncTask{
        ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(MainActivity.this,android.R.style.Theme_DeviceDefault_Dialog);
            mDialog.setCancelable(false);
            mDialog.setMessage("Please Wait");
            mDialog.show();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            HttpClient client = new HttpClient();
            client.get(API_GET_TOKEN, new HttpResponseCallback() {
                @Override
                public void success(final String responseBody) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //hide group waiting
                            group_waiting.setVisibility(View.GONE);
                            //show group payment
                            group_payment.setVisibility(View.VISIBLE);

                            // set Token
                            token = responseBody;
                        }
                    });
                }

                @Override
                public void failure(Exception exception) {
                    Log.d("ABHI_ERROR",exception.toString());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            mDialog.dismiss();
        }
    }
}
