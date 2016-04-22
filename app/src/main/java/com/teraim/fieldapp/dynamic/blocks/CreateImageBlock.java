/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import java.io.InputStream;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

/**
 * @author Terje
 *
 */
public class CreateImageBlock extends Block implements EventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5781622495945524716L;
	private final String name;
	private final String container;
	private final String source;
	private final String scale;
	private ImageView img = null;
	private WF_Context myContext;
	private boolean isVisible;
	private String dynImgName;
	private List<EvalExpr> sourceE;

	public CreateImageBlock(String id, String nName, String container,
			String source, String scale, boolean isVisible) {
		this.blockId=id;
		this.name=nName;
		this.container = container;
		this.sourceE=Expressor.preCompileExpression(source);
		this.source=source;
		this.scale = scale;
		this.isVisible = isVisible;
		
	}


	public void create(WF_Context myContext) {
		this.myContext = myContext;
		o = GlobalState.getInstance().getLogger();
		WF_Container myContainer = (WF_Container)myContext.getContainer(container);
		Log.d("vortex","Source name is "+source);
		if (myContainer != null && sourceE!=null) {
			dynImgName = Expressor.analyze(sourceE);
			ScaleType scaleT=ScaleType.FIT_XY;
			img = new ImageView(myContext.getContext());
			if (Tools.isURL(dynImgName)) {
				new DownloadImageTask(img).execute(dynImgName);
			} else {
				setImageFromFile(myContext);
			}
			Log.d("vortex","Dynamic name is "+dynImgName);
			if (scale!=null || scale.length()>0)
				scaleT = ScaleType.valueOf(scale.toUpperCase());
			img.setScaleType(scaleT);
			WF_Widget myWidget= new WF_Widget(blockId,img,isVisible,myContext);	
			myContainer.add(myWidget);
			myContext.addEventListener(this, EventType.onActivityResult);
		} else {
			if (source==null || sourceE == null) {
				o.addRow("");
				o.addRedText("Failed to add image with block id "+blockId+" - source is either null or evaluates to null: "+source);				
			}
			o.addRow("");
			o.addRedText("Failed to add image with block id "+blockId+" - missing container "+container);
		}
	}



	private void setImageFromFile(WF_Context myContext) {
		if (dynImgName==null) {
			Log.e("vortex","no dynimage name in createimageblock... exit");
		}
		final int divisor = 1;
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		Bitmap bip = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR+dynImgName,options);		
		int realW = options.outWidth;
		int realH = options.outHeight;
		if (realW>0) {
			double ratio = realH/realW;
			Display display = myContext.getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int sWidth = size.x;
			double tWidth = sWidth/divisor;
			int tHeight = (int) (tWidth*ratio);
			options.inSampleSize = Tools.calculateInSampleSize(options, (int)tWidth, tHeight);
			options.inJustDecodeBounds = false;
			bip = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR+dynImgName,options);
			if (bip!=null)
				img.setImageBitmap(bip);
			else
				Log.d("nils","Could not decode image "+dynImgName);
		}
		else {
			Log.d("nils","Did not find picture "+dynImgName);
		}
	}



	class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if (result!=null)
				bmImage.setImageBitmap(result);
		}
	}



	@Override
	public void onEvent(Event e) {
		Log.d("vortex","Img was taken");
		setImageFromFile(myContext);
	}
}
