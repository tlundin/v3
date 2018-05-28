package com.teraim.fieldapp.dynamic.blocks;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;

import java.io.File;
import java.io.IOException;

import static com.teraim.fieldapp.non_generics.Constants.PIC_ROOT_DIR;

public class StartCameraBlock extends Block implements EventListener {

    private static final long serialVersionUID = -8381530803516157092L;

    private String fileName;
    private int TAKE_PHOTO_CODE = 0;
    private WF_Context myContext;

    public StartCameraBlock(String id, String fileName) {
        this.blockId = id;
        this.fileName=fileName;
    }

    public void create(WF_Context myContext) {

        File newfile = new File(PIC_ROOT_DIR+fileName);
        try {
            newfile.createNewFile();


            Uri outputFileUri = Uri.fromFile(newfile);

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            this.myContext=myContext;
            myContext.getActivity().startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
        }
        catch (IOException e)
        {
            Log.e("vortex","failed to create image file.");
        }

    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == Event.EventType.onActivityResult) {
            Log.d("vortex","picture saved:  "+fileName);
            myContext.registerEvent(new WF_Event_OnSave("photo"));
        }
    }

    @Override
    public String getName() {
        return "camerablock";
    }

}
