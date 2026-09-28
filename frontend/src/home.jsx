import Header from "./components/header"
import { useNavigate } from 'react-router-dom';
import './styles/homestyles.css'

export default function home(){
    const navigate = useNavigate();
    return(
        <>
            <Header />
            <div className="reglog">
            <h1>Welcome to Club Room Booking</h1>
            
                <button onClick={()=>navigate('/register')}>Register</button>
                <button onClick={()=>navigate('/login')}>Login</button>
            </div>
        </>
    );
}