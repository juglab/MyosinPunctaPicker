package de.csbd.learnathon.command;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

public class FileChooser {

	public static void main( String[] args ) {

		JFileChooser jfc = new JFileChooser( FileSystemView.getFileSystemView().getHomeDirectory() );

		int returnValue = jfc.showSaveDialog( null );

		if ( returnValue == JFileChooser.APPROVE_OPTION ) {
			File selectedFile = jfc.getSelectedFile();
			System.out.println( selectedFile.getAbsolutePath() );
		}

	}

}
