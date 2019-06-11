package repository

import fujitask.eff.Fujitask.Transaction

//import fujitask.eff.Fujitask.Session

//trait Transaction

trait ReadTransaction extends Transaction

trait ReadWriteTransaction extends ReadTransaction