/*
 * Copyright (c) 2006 Rick Mugridge, www.RimuResearch.com
 * Released under the terms of the GNU General Public License version 2 or later.
 * Written: 19/08/2006
*/

package fitlibrary.specify.exception;

import fitlibrary.traverse.DomainAdapter;

public class ExceptionThrownByEquals implements DomainAdapter  {
	public User user() {
		return new User();
	}
	public User findUser(@SuppressWarnings("unused") String s) {
		return new User();
	}
	public static class User {
		@Override
		public boolean equals(@SuppressWarnings("unused") Object object) {
			throw new ForcedException();
		}
	}
	public Object getSystemUnderTest() {
		return null;
	}
}
