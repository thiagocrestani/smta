package mei.tcd.filtros;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

public class Kalman {
	DenseMatrix64F P=new DenseMatrix64F(15,15); //Matriz covariancia dos estados
	
	DenseMatrix64F R=new DenseMatrix64F(3,3); // Matriz de erros de medi��es
}
