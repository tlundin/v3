/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

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
			Log.d("botox","my image name before: "+dynImgName);

			ScaleType scaleT=ScaleType.FIT_XY;
			img = new ImageView(myContext.getContext());
			if (Tools.isURL(dynImgName)) {
				new DownloadImageTask(img).execute(dynImgName);
			} else {
				//Try to parse as regexp.
				String fileName = this.figureOutFileToLoad(dynImgName);
				if (fileName==null) {
					Log.d("botox","Failed to find file using "+dynImgName+" as regexp pattern");
				} else {
					Log.d("botox","Filename now: "+fileName);
					dynImgName=fileName;
				}

				setImageFromFile(myContext,img);
			}

			if (scale!=null || scale.length()>0)
				scaleT = ScaleType.valueOf(scale.toUpperCase());
			img.setScaleType(scaleT);
			WF_Widget myWidget= new WF_Widget(blockId,img,isVisible,myContext);	
			myContainer.add(myWidget);
			myContext.registerEventListener(this, EventType.onActivityResult);
		} else {
			if (source==null || sourceE == null) {
				o.addRow("");
				o.addRedText("Failed to add image with block id "+blockId+" - source is either null or evaluates to null: "+source);				
			}
			o.addRow("");
			o.addRedText("Failed to add image with block id "+blockId+" - missing container "+container);
		}
		img.setClickable(true);
		img.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showImage();
			}
		});
	}

	public void showImage() {
		Dialog builder = new Dialog(myContext.getContext());
		builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
		builder.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
				//nothing;
			}
		});

		ImageView imageView = new ImageView(myContext.getContext());
		if (Tools.isURL(dynImgName)) {
			new DownloadImageTask(imageView).execute(dynImgName);
		} else {
			setImageFromFile(myContext,imageView);
		}
		builder.addContentView(imageView, new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		builder.show();
	}


	private void setImageFromFile(WF_Context myContext, ImageView img) {
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

	private String figureOutFileToLoad(String pattern) {

		String fileName = null;

		File f = new File(Constants.PIC_ROOT_DIR);
		if (f.exists() && f.isDirectory() && pattern!=null)

		{
			final Pattern p = Pattern.compile(pattern);
			File[] flists = f.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					//p.matcher(file.getName()).matches();
					//Log.e("botox", "patternmatch " + p.matcher(file.getName()).matches());

					return p.matcher(file.getName()).matches();

				}
			});

			if (flists!=null && flists.length>0) {
				Log.d("botox","found file matches for pattern "+pattern);
				for (File fl:flists) {
					Log.d("vortex",fl.getName());
				}
				return flists[0].getName();
			}
		}

		return null;
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
		String fileName = this.figureOutFileToLoad(dynImgName);
		if (fileName!=null)
			dynImgName=fileName;
		setImageFromFile(myContext,img);
	}

	@Override
	public String getName() {
		return "CREATE IMAGE BLOCK ";
	}
}
