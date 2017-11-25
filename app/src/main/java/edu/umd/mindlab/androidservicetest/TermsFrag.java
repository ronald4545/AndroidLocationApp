package edu.umd.mindlab.androidservicetest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

/**
 * Created by User on 11/8/2017.
 */

public class TermsFrag extends android.support.v4.app.Fragment {

    private final String TAG = "Consent Activity";

    private Button backToMain;
    String pdfFileName;
    Integer pageNumber = 0;

    public static final String SAMPLE_FILE = "Consent_Smartphone_App2.pdf";
    PDFView pdfView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        // Defines the xml file for the fragment
        return inflater.inflate(R.layout.terms_layout, parent, false);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);

        // maybe I should put this in onViewCreated
        pdfView = (PDFView) getView().findViewById(R.id.pdfView3);

        displayFromAsset(SAMPLE_FILE);

        backToMain = (Button) getView().findViewById(R.id.backToMain);
        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                getActivity().getSupportFragmentManager().beginTransaction().remove(TermsFrag.this).commit();
            }
        });

    }

    // get the pdf from the assets file and display it
    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;

        pdfView.fromAsset(SAMPLE_FILE)
                .defaultPage(pageNumber)
                .enableSwipe(true)

                .swipeHorizontal(false)
                .enableAnnotationRendering(true)
                // is the below line important? does it make it vertically scrollable?
                //.scrollHandle(new DefaultScrollHandle(this))
                .load();

    }
}
